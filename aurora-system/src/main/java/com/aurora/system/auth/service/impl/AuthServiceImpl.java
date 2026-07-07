package com.aurora.system.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.util.RedisUtil;
import com.aurora.system.auth.model.req.LoginReq;
import com.aurora.system.auth.model.resp.CaptchaResp;
import com.aurora.system.auth.model.resp.LoginResp;
import com.aurora.system.auth.model.resp.UserInfoResp;
import com.aurora.system.auth.service.AuthService;
import com.aurora.system.menu.service.SysMenuService;
import com.aurora.system.user.entity.SysUserDO;
import com.aurora.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 认证业务实现（Sa-Token + easy-captcha + BCrypt）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final SysUserMapper userMapper;
    private final RedisUtil redisUtil;
    private final SysMenuService menuService;

    @Value("${aurora.captcha.expire-seconds:120}")
    private long captchaExpireSeconds;

    @Value("${aurora.captcha.key-prefix:captcha:}")
    private String captchaKeyPrefix;

    @Override
    public CaptchaResp captcha() {
        // hutool LineCaptcha：4 位（数字+字母），200x70
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 70, 4, 30);
        String code = captcha.getCode();
        String uuid = IdUtil.fastSimpleUUID();

        // Redis 存验证码（小写），TTL 120s
        String key = captchaKeyPrefix + uuid;
        redisUtil.set(key, code.toLowerCase(), Duration.ofSeconds(captchaExpireSeconds));

        // base64 编码
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        captcha.write(bos);
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        IoUtil.close(bos);

        return CaptchaResp.builder()
                .uuid(uuid)
                .img(base64)
                .build();
    }

    @Override
    public LoginResp login(LoginReq req) {
        // 1. 校验验证码（先校验再查用户，避免无谓打 DB）
        String key = captchaKeyPrefix + req.getUuid();
        String cached = redisUtil.getString(key);
        // 用后即删（一次性，防重放）
        redisUtil.delete(key);
        if (cached == null || !cached.equalsIgnoreCase(req.getCaptcha())) {
            throw new BizException(BizCode.PARAM_ERROR.getCode(), "验证码错误或已失效");
        }

        // 2. 查用户
        SysUserDO user = userMapper.selectOne(
                Wrappers.<SysUserDO>lambdaQuery().eq(SysUserDO::getUsername, req.getUsername()));
        // 用户不存在 / 密码错误：返回同样消息，不泄露用户是否存在
        if (user == null || !PASSWORD_ENCODER.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(BizCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }
        // 3. 状态校验
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BizException(BizCode.USER_DISABLED.getCode(), BizCode.USER_DISABLED.getMsg());
        }

        // 4. 查角色
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());

        // 5. Sa-Token 登录 + 写 session
        StpUtil.login(user.getId());
        SaSession session = StpUtil.getSession();
        session.set("username", user.getUsername());
        session.set("roles", roleCodes);

        log.info("[登录成功] userId={} username={} roles={}", user.getId(), user.getUsername(), roleCodes);

        return LoginResp.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .build();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserInfoResp userInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BizCode.USER_NOT_FOUND.getCode(), BizCode.USER_NOT_FOUND.getMsg());
        }
        SaSession session = StpUtil.getSession();
        Object rolesObj = session.get("roles");
        @SuppressWarnings("unchecked")
        List<String> roles = rolesObj instanceof List ? (List<String>) rolesObj : List.of();

        return UserInfoResp.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(menuService.currentUserPermissions(userId))
                .build();
    }
}
