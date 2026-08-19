package com.aurora.ai.knowledge.graph;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Neo4j 连接配置。默认关闭；开启后通过官方 HTTP 事务接口
 * （{@code /db/neo4j/tx/commit}）以 Cypher over JSON 方式访问，
 * 不引入原生 Java Driver 依赖，离线构建友好。
 */
@Data
@Component
public class Neo4jProperties {

    @Value("${aurora.ai.neo4j.enabled:false}")
    private boolean enabled;

    @Value("${aurora.ai.neo4j.uri:http://localhost:7474}")
    private String uri;

    @Value("${aurora.ai.neo4j.username:neo4j}")
    private String username;

    @Value("${aurora.ai.neo4j.password:neo4j}")
    private String password;
}
