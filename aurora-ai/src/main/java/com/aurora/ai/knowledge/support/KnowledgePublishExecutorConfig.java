package com.aurora.ai.knowledge.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 文档异步发布的专用线程池。
 *
 * <p>背景：上传接口原为同步等待「切分 + 向量化 + 图谱抽取」全部完成才返回，
 * 文档一分块多就会撞上前端 30 秒超时。改为提交到本线程池后台处理，接口立即返回。</p>
 *
 * <p>并发刻意压得低（核心 2）：embedding 供应商普遍有速率限制，
 * 并发过高反而触发限流导致整批失败；队列用于承接瞬时批量上传。</p>
 */
@Configuration
public class KnowledgePublishExecutorConfig {

    public static final String KNOWLEDGE_PUBLISH_EXECUTOR = "knowledgePublishExecutor";

    @Bean(KNOWLEDGE_PUBLISH_EXECUTOR)
    public Executor knowledgePublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("knowledge-publish-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
