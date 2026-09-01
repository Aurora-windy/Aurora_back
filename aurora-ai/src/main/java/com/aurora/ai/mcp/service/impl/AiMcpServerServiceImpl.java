package com.aurora.ai.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.ai.mcp.client.McpClientManager;
import com.aurora.ai.mcp.entity.AiMcpServerDO;
import com.aurora.ai.mcp.mapper.AiMcpServerMapper;
import com.aurora.ai.mcp.model.req.McpServerEnabledReq;
import com.aurora.ai.mcp.model.req.McpServerPageReq;
import com.aurora.ai.mcp.model.req.McpServerSaveReq;
import com.aurora.ai.mcp.model.resp.McpServerResp;
import com.aurora.ai.mcp.model.resp.McpServerSyncResp;
import com.aurora.ai.mcp.service.AiMcpServerService;
import com.aurora.ai.provider.support.AiSecretCipher;
import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolRegistry;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP Server 配置服务实现（T-M2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMcpServerServiceImpl implements AiMcpServerService {

    private final AiMcpServerMapper mapper;
    private final McpClientManager clientManager;
    private final AiToolRegistry toolRegistry;

    @Override
    public PageResult<McpServerResp> page(McpServerPageReq req) {
        Page<AiMcpServerDO> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<AiMcpServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(req.getCode()), AiMcpServerDO::getCode, req.getCode())
                .like(StringUtils.hasText(req.getName()), AiMcpServerDO::getName, req.getName())
                .eq(req.getEnabled() != null, AiMcpServerDO::getEnabled, req.getEnabled())
                .orderByDesc(AiMcpServerDO::getCreateTime);
        Page<AiMcpServerDO> result = mapper.selectPage(page, wrapper);
        List<McpServerResp> list = result.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(McpServerSaveReq req) {
        // code 唯一性检查
        LambdaQueryWrapper<AiMcpServerDO> check = new LambdaQueryWrapper<>();
        check.eq(AiMcpServerDO::getCode, req.getCode());
        if (mapper.selectCount(check) > 0) {
            throw new BizException(BizCode.OPERATION_FAIL, "MCP server code already exists: " + req.getCode());
        }

        AiMcpServerDO entity = new AiMcpServerDO();
        entity.setCode(req.getCode());
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setBaseUrl(req.getBaseUrl());
        entity.setBearerTokenCipher(AiSecretCipher.encrypt(req.getBearerToken()));
        entity.setTimeoutSeconds(req.getTimeoutSeconds() != null ? req.getTimeoutSeconds() : 10);
        entity.setEnabled(1); // 新建默认启用
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, McpServerSaveReq req) {
        AiMcpServerDO entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(BizCode.OPERATION_FAIL, "MCP server not found: " + id);
        }
        // code 变更时检查唯一性
        if (!entity.getCode().equals(req.getCode())) {
            LambdaQueryWrapper<AiMcpServerDO> check = new LambdaQueryWrapper<>();
            check.eq(AiMcpServerDO::getCode, req.getCode());
            if (mapper.selectCount(check) > 0) {
                throw new BizException(BizCode.OPERATION_FAIL, "MCP server code already exists: " + req.getCode());
            }
        }

        entity.setCode(req.getCode());
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setBaseUrl(req.getBaseUrl());
        if (StringUtils.hasText(req.getBearerToken())) {
            entity.setBearerTokenCipher(AiSecretCipher.encrypt(req.getBearerToken()));
        }
        if (req.getTimeoutSeconds() != null) {
            entity.setTimeoutSeconds(req.getTimeoutSeconds());
        }
        mapper.updateById(entity);

        // 如果已启用，重新同步工具
        if (entity.getEnabled() == 1) {
            syncInternal(entity, true);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(Long id, McpServerEnabledReq req) {
        AiMcpServerDO entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(BizCode.OPERATION_FAIL, "MCP server not found: " + id);
        }
        entity.setEnabled(req.getEnabled());
        mapper.updateById(entity);

        if (req.getEnabled() == 1) {
            // 启用：同步工具
            syncInternal(entity, true);
        } else {
            // 禁用：注销工具 + 断开连接
            List<String> toolNames = clientManager.getToolNames(entity.getCode());
            toolRegistry.unregisterMcpTools(toolNames);
            clientManager.disconnect(entity.getCode());
            entity.setToolCount(0);
            entity.setLastSyncAt(null);
            mapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiMcpServerDO entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(BizCode.OPERATION_FAIL, "MCP server not found: " + id);
        }
        // 先注销工具 + 断开连接
        List<String> toolNames = clientManager.getToolNames(entity.getCode());
        toolRegistry.unregisterMcpTools(toolNames);
        clientManager.disconnect(entity.getCode());
        mapper.deleteById(id);
    }

    @Override
    public McpServerSyncResp sync(Long id) {
        AiMcpServerDO entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(BizCode.OPERATION_FAIL, "MCP server not found: " + id);
        }
        return syncInternal(entity, false);
    }

    @Override
    public void syncAllEnabled() {
        LambdaQueryWrapper<AiMcpServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpServerDO::getEnabled, 1);
        List<AiMcpServerDO> servers = mapper.selectList(wrapper);
        for (AiMcpServerDO server : servers) {
            try {
                syncInternal(server, true);
            } catch (Exception ex) {
                log.warn("mcp.syncAll failed server={} error={}", server.getCode(), ex.getMessage());
                // 单个 server 失败不影响其他
            }
        }
    }

    @Override
    public void disconnectAllEnabled() {
        LambdaQueryWrapper<AiMcpServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpServerDO::getEnabled, 1);
        for (AiMcpServerDO server : mapper.selectList(wrapper)) {
            List<String> toolNames = clientManager.getToolNames(server.getCode());
            toolRegistry.unregisterMcpTools(toolNames);
            clientManager.disconnect(server.getCode());
        }
    }

    @Override
    public List<McpServerResp> listEnabled() {
        LambdaQueryWrapper<AiMcpServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpServerDO::getEnabled, 1);
        return mapper.selectList(wrapper).stream().map(this::toResp).toList();
    }

    /**
     * 内部同步逻辑：连接 server → 同步工具 → 注册到 registry → 更新 entity。
     *
     * @param suppressError true=失败时只记录日志不抛异常（启动时批量同步用）
     */
    private McpServerSyncResp syncInternal(AiMcpServerDO entity, boolean suppressError) {
        long started = System.currentTimeMillis();
        try {
            // 先注销旧工具
            List<String> oldToolNames = clientManager.getToolNames(entity.getCode());
            toolRegistry.unregisterMcpTools(oldToolNames);

            // 连接并同步
            List<AiToolDefinition> tools = clientManager.connectAndSync(entity);
            toolRegistry.registerMcpTools(tools);

            // 更新 entity
            entity.setToolCount(tools.size());
            entity.setLastSyncAt(LocalDateTime.now());
            entity.setLastError(null);
            mapper.updateById(entity);

            return McpServerSyncResp.success(tools.size(), System.currentTimeMillis() - started);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - started;
            String errorMsg = ex.getMessage();
            log.warn("mcp.syncInternal failed server={} duration={}ms error={}", entity.getCode(), duration, errorMsg);

            // 记录错误到 entity
            entity.setLastError(errorMsg != null && errorMsg.length() > 900 ? errorMsg.substring(0, 900) + "..." : errorMsg);
            mapper.updateById(entity);

            if (suppressError) {
                return McpServerSyncResp.fail(errorMsg, duration);
            }
            throw new BizException(BizCode.OPERATION_FAIL, "MCP sync failed: " + errorMsg);
        }
    }

    private McpServerResp toResp(AiMcpServerDO entity) {
        McpServerResp resp = new McpServerResp();
        resp.setId(entity.getId());
        resp.setCode(entity.getCode());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setBaseUrl(entity.getBaseUrl());
        resp.setHasBearerToken(StringUtils.hasText(entity.getBearerTokenCipher()));
        resp.setTimeoutSeconds(entity.getTimeoutSeconds());
        resp.setEnabled(entity.getEnabled());
        resp.setToolCount(entity.getToolCount());
        resp.setLastSyncAt(entity.getLastSyncAt());
        resp.setLastError(entity.getLastError());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }
}
