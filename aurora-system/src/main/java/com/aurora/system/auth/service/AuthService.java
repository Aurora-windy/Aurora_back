package com.aurora.system.auth.service;

import com.aurora.system.auth.model.req.LoginReq;
import com.aurora.system.auth.model.resp.CaptchaResp;
import com.aurora.system.auth.model.resp.LoginResp;
import com.aurora.system.auth.model.resp.UserInfoResp;

/**
 * 认证业务接口
 */
public interface AuthService {

    /** 生成图形验证码 */
    CaptchaResp captcha();

    /** 账号密码登录 */
    LoginResp login(LoginReq req);

    /** 登出 */
    void logout();

    /** 获取当前登录用户信息 */
    UserInfoResp userInfo();
}
