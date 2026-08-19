package com.aurora.ai.knowledge.graph;

import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识图谱服务（Neo4j）。
 * <p>职责：文档入库后用 LLM 抽取 (实体,关系,实体) 三元组写入图库；
 * 对话检索时用查询实体在图中做邻居扩展，把结构化关系作为补充上下文注入 prompt。</p>
 * <p>所有外部调用均 try-catch 降级——Neo4j 未启用或不可达时静默跳过，绝不打挂主链路。</p>
 */
@Slf4j
@Component
public class Neo4jGraphService {

    private static final String DB_TX = "/db/neo4j/tx/commit";
    private static final String USAGE_CHAT = "CHAT";
    private static final String USAGE_BOTH = "BOTH";

    private final Neo4jProperties properties;
    private final ObjectMapper objectMapper;
    private final AiModelProviderMapper providerMapper;
    private final OpenAiClientFactory openAiClientFactory;

    private volatile RestClient neo4jClient;

    public Neo4jGraphService(Neo4jProperties properties, ObjectMapper objectMapper,
                             AiModelProviderMapper providerMapper, OpenAiClientFactory openAiClientFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.providerMapper = providerMapper;
        this.openAiClientFactory = openAiClientFactory;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** 文档发布后抽取三元组入库（失败仅告警，不影响主流程） */
    public void extractAndStore(Long docId, String title, String content) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            List<Triple> triples = parseTriples(extractTriples(content));
            for (Triple t : triples) {
                runCypher("MERGE (s:Entity {name:$s}) MERGE (o:Entity {name:$o}) "
                                + "MERGE (s)-[:REL {type:$r, docId:$docId}]->(o)",
                        Map.of("s", t.subject(), "o", t.object(), "r", t.relation(), "docId", String.valueOf(docId)));
            }
            log.info("Neo4j 图谱抽取完成 docId={} triples={}", docId, triples.size());
        } catch (Exception e) {
            log.warn("Neo4j 图谱抽取失败 docId={}: {}", docId, e.getMessage());
        }
    }

    /** 检索阶段：用查询实体在图中做邻居扩展，返回补充上下文文本 */
    public Optional<String> retrieveContext(String query) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            List<String> entities = extractEntities(query);
            if (entities.isEmpty()) {
                return Optional.empty();
            }
            String cypher = "MATCH (e:Entity)-[r]-(n:Entity) WHERE e.name IN $names "
                    + "RETURN e.name AS subject, r.type AS relation, n.name AS object LIMIT 25";
            Map<String, Object> resp = runCypherRaw(cypher, Map.of("names", entities));
            return Optional.ofNullable(formatContext(resp));
        } catch (Exception e) {
            log.warn("Neo4j 图谱检索失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String formatContext(Map<String, Object> resp) {
        List<Map<String, Object>> results = castList(resp.get("results"));
        if (results == null || results.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> data = castList(results.get(0).get("data"));
        if (data == null || data.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : data) {
            List<Object> rowVals = castList(row.get("row"));
            if (rowVals != null && rowVals.size() == 3) {
                sb.append("- ").append(rowVals.get(0)).append(" ")
                        .append(rowVals.get(1)).append(" ")
                        .append(rowVals.get(2)).append("\n");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private Map<String, Object> runCypherRaw(String statement, Map<String, Object> params) {
        Map<String, Object> body = Map.of("statements", List.of(
                Map.of("statement", statement, "parameters", params)));
        String resp = client().post().uri(DB_TX)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(resp, Map.class);
            return map;
        } catch (Exception e) {
            log.warn("Neo4j 响应解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private void runCypher(String statement, Map<String, Object> params) {
        Map<String, Object> resp = runCypherRaw(statement, params);
        List<Map<String, Object>> errors = castList(resp.get("errors"));
        if (errors != null && !errors.isEmpty()) {
            log.warn("Neo4j 执行错误: {}", errors);
        }
    }

    private RestClient client() {
        if (neo4jClient == null) {
            synchronized (this) {
                if (neo4jClient == null) {
                    String auth = Base64.getEncoder().encodeToString(
                            (properties.getUsername() + ":" + properties.getPassword()).getBytes(StandardCharsets.UTF_8));
                    String base = properties.getUri();
                    if (base.endsWith("/")) {
                        base = base.substring(0, base.length() - 1);
                    }
                    neo4jClient = RestClient.builder().baseUrl(base)
                            .defaultHeader("Authorization", "Basic " + auth)
                            .build();
                }
            }
        }
        return neo4jClient;
    }

    private String extractTriples(String content) {
        AiModelProviderDO provider = chatProvider();
        if (provider == null) {
            return "[]";
        }
        String text = content.length() > 4000 ? content.substring(0, 4000) : content;
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "你是知识图谱抽取器。从文本中抽取实体关系三元组，仅输出 JSON 数组，"
                                + "格式：[{\"subject\":\"实体1\",\"relation\":\"关系\",\"object\":\"实体2\"}]，不要任何解释。"),
                Map.of("role", "user", "content", text));
        try {
            return openAiClientFactory.chatCompletion(provider, messages, 0.0, 1500);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Triple> parseTriples(String json) {
        List<Triple> out = new ArrayList<>();
        try {
            int s = json.indexOf('[');
            int e = json.lastIndexOf(']');
            if (s < 0 || e < 0) {
                return out;
            }
            JsonNode arr = objectMapper.readTree(json.substring(s, e + 1));
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(new Triple(n.path("subject").asText(), n.path("relation").asText(), n.path("object").asText()));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private List<String> extractEntities(String query) {
        AiModelProviderDO provider = chatProvider();
        if (provider == null) {
            return List.of();
        }
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "从用户问题中抽取涉及的知识实体名称（人物/组织/概念/地点），"
                                + "仅输出 JSON 数组字符串如 [\"实体A\",\"实体B\"]，无实体则输出 []。"),
                Map.of("role", "user", "content", query));
        try {
            String r = openAiClientFactory.chatCompletion(provider, messages, 0.0, 300);
            int s = r.indexOf('[');
            int e = r.lastIndexOf(']');
            if (s < 0 || e < 0) {
                return List.of();
            }
            JsonNode arr = objectMapper.readTree(r.substring(s, e + 1));
            List<String> list = new ArrayList<>();
            if (arr.isArray()) {
                arr.forEach(n -> list.add(n.asText()));
            }
            return list;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private AiModelProviderDO chatProvider() {
        return providerMapper.selectOne(Wrappers.<AiModelProviderDO>lambdaQuery()
                .eq(AiModelProviderDO::getEnabled, 1)
                .in(AiModelProviderDO::getUsageType, USAGE_CHAT, USAGE_BOTH)
                .orderByAsc(AiModelProviderDO::getSortOrder)
                .last("LIMIT 1"));
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object o) {
        return o instanceof List ? (List<T>) o : null;
    }

    // ==================== 图谱可视化 / 管理接口（供 REST 层调用） ====================

    /** 图谱概览信息 */
    public record GraphInfo(String status, String databaseName, long entityCount, long relationshipCount) {
    }

    /** 图谱节点（id 即实体名，Neo4j 中按名称 MERGE 唯一） */
    public record GraphNode(String id, String name) {
    }

    /** 图谱边 */
    public record GraphEdge(String source, String target, String type) {
    }

    /** 图谱数据（节点 + 边） */
    public record GraphData(List<GraphNode> nodes, List<GraphEdge> edges) {
    }

    /** 图谱连接状态与规模概览（未启用/不可达时返回 closed 空数据，不打挂调用方） */
    public GraphInfo graphInfo() {
        if (!properties.isEnabled()) {
            return new GraphInfo("closed", "Neo4j 未启用", 0, 0);
        }
        try {
            List<Map<String, Object>> results = runCypherBatch(List.of(
                    "MATCH (n:Entity) RETURN count(n) AS c",
                    "MATCH ()-[r:REL]->() RETURN count(r) AS c"));
            long entity = firstScalar(results, 0, 0L);
            long rel = firstScalar(results, 1, 0L);
            return new GraphInfo("open", "Neo4j", entity, rel);
        } catch (Exception e) {
            log.warn("Neo4j 图谱信息获取失败: {}", e.getMessage());
            return new GraphInfo("closed", "Neo4j 不可达", 0, 0);
        }
    }

    /** 随机采样 num 个实体及其内部关系（用于图谱可视化初始渲染） */
    public GraphData sampleNodes(int num) {
        if (!properties.isEnabled()) {
            return emptyGraph();
        }
        int limit = Math.max(1, Math.min(num, 500));
        try {
            Map<String, Object> resp = runCypherRaw(
                    "MATCH (n:Entity) RETURN n.name AS name LIMIT $num", Map.of("num", limit));
            List<String> names = firstColumn(resp);
            if (names.isEmpty()) {
                return emptyGraph();
            }
            Map<String, Object> edgeResp = runCypherRaw(
                    "MATCH (s:Entity)-[r:REL]->(t:Entity) WHERE s.name IN $names AND t.name IN $names "
                            + "RETURN s.name AS source, r.type AS type, t.name AS target",
                    Map.of("names", names));
            return toGraphData(edgeResp);
        } catch (Exception e) {
            log.warn("Neo4j 采样失败: {}", e.getMessage());
            return emptyGraph();
        }
    }

    /** 按实体名模糊检索：返回命中实体及其一跳邻居 */
    public GraphData searchEntity(String name) {
        if (!properties.isEnabled() || !StringUtils.hasText(name)) {
            return emptyGraph();
        }
        try {
            Map<String, Object> resp = runCypherRaw(
                    "MATCH (e:Entity)-[r:REL]-(n:Entity) WHERE e.name CONTAINS $name "
                            + "RETURN e.name AS source, r.type AS type, n.name AS target LIMIT 300",
                    Map.of("name", name.trim()));
            return toGraphData(resp);
        } catch (Exception e) {
            log.warn("Neo4j 实体检索失败: {}", e.getMessage());
            return emptyGraph();
        }
    }

    /** 删除指定实体及其全部关联边 */
    public boolean deleteEntity(String name) {
        if (!properties.isEnabled() || !StringUtils.hasText(name)) {
            return false;
        }
        try {
            runCypher("MATCH (e:Entity) WHERE e.name = $name DETACH DELETE e", Map.of("name", name.trim()));
            return true;
        } catch (Exception e) {
            log.warn("Neo4j 删除实体失败: {}", e.getMessage());
            return false;
        }
    }

    /** 清空整个图谱 */
    public boolean deleteAll() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            runCypher("MATCH (n:Entity) DETACH DELETE n", Map.of());
            return true;
        } catch (Exception e) {
            log.warn("Neo4j 清空图谱失败: {}", e.getMessage());
            return false;
        }
    }

    private GraphData emptyGraph() {
        return new GraphData(List.of(), List.of());
    }

    /** 一次事务提交多条只读语句，返回各语句的 results */
    private List<Map<String, Object>> runCypherBatch(List<String> statements) {
        List<Map<String, Object>> stmts = new ArrayList<>();
        for (String s : statements) {
            stmts.add(Map.of("statement", s));
        }
        Map<String, Object> body = Map.of("statements", stmts);
        try {
            String resp = client().post().uri(DB_TX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(resp, Map.class);
            List<Map<String, Object>> results = castList(map.get("results"));
            return results == null ? List.of() : results;
        } catch (Exception e) {
            log.warn("Neo4j 批量查询失败: {}", e.getMessage());
            return List.of();
        }
    }

    private long firstScalar(List<Map<String, Object>> results, int idx, long def) {
        try {
            if (results.size() <= idx) {
                return def;
            }
            List<Map<String, Object>> data = castList(results.get(idx).get("data"));
            if (data == null || data.isEmpty()) {
                return def;
            }
            List<Object> row = castList(data.get(0).get("row"));
            if (row == null || row.isEmpty()) {
                return def;
            }
            Number n = (Number) row.get(0);
            return n == null ? def : n.longValue();
        } catch (Exception e) {
            return def;
        }
    }

    /** 取结果首列去重后的字符串列表（用于节点名集合） */
    private List<String> firstColumn(Map<String, Object> resp) {
        List<String> out = new ArrayList<>();
        List<Map<String, Object>> results = castList(resp.get("results"));
        if (results == null || results.isEmpty()) {
            return out;
        }
        List<Map<String, Object>> data = castList(results.get(0).get("data"));
        if (data == null) {
            return out;
        }
        for (Map<String, Object> rowMap : data) {
            List<Object> row = castList(rowMap.get("row"));
            if (row != null && !row.isEmpty()) {
                String v = String.valueOf(row.get(0));
                if (!out.contains(v)) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    /** 把 (source, type, target) 结果行转成图数据（节点去重保序） */
    private GraphData toGraphData(Map<String, Object> resp) {
        List<GraphEdge> edges = new ArrayList<>();
        Map<String, GraphNode> seen = new LinkedHashMap<>();
        List<Map<String, Object>> results = castList(resp.get("results"));
        if (results == null || results.isEmpty()) {
            return emptyGraph();
        }
        List<Map<String, Object>> data = castList(results.get(0).get("data"));
        if (data == null) {
            return emptyGraph();
        }
        for (Map<String, Object> rowMap : data) {
            List<Object> row = castList(rowMap.get("row"));
            if (row == null || row.size() < 3) {
                continue;
            }
            String source = String.valueOf(row.get(0));
            String type = String.valueOf(row.get(1));
            String target = String.valueOf(row.get(2));
            seen.computeIfAbsent(source, k -> new GraphNode(source, source));
            seen.computeIfAbsent(target, k -> new GraphNode(target, target));
            edges.add(new GraphEdge(source, target, type));
        }
        return new GraphData(new ArrayList<>(seen.values()), edges);
    }

    /** 三元组 */
    public record Triple(String subject, String relation, String object) {
    }
}
