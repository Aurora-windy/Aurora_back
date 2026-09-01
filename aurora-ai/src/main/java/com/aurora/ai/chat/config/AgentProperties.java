package com.aurora.ai.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 2.0 运行参数（T1 详设 §9）。字段随任务落地逐步启用：
 * T3 用 history*；T4 用 fc/max/token/fastpath/toolResult。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aurora.ai.agent")
public class AgentProperties {

    /** FC 总开关；false = 完全回退现状行为（正则 + 纯聊天） */
    private boolean fcEnabled = true;

    /** FC 循环轮数上限 */
    private int maxRounds = 5;

    /** FC 循环累计 token 硬顶（估算，char/2） */
    private int tokenBudget = 8000;

    /** 多轮记忆窗口：最多保留的轮数（一轮 = user + assistant） */
    private int historyRounds = 10;

    /** 多轮记忆窗口：字符预算，与轮数先到为准 */
    private int historyMaxChars = 4000;

    /** 快速路径工具结果是否交给 LLM 流式归纳；false = 现状静态摘要文案 */
    private boolean fastpathSummarize = true;

    /** tool 消息回传模型的单次结果截断长度（字符） */
    private int toolResultMaxChars = 2000;

}
