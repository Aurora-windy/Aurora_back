package com.aurora.ai.agent.service;

import com.aurora.ai.agent.model.resp.ActionResultResp;

public interface AgentActionService {
    ActionResultResp confirm(Long actionId);

    ActionResultResp reject(Long actionId);
}