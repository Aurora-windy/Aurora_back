package com.aurora.ai.knowledge.graph;

import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private static final String USAGE_GRAPH = "GRAPH";

    /** Neo4j 不可达时不能无限等待，否则上传请求被挂死、前端只看到超时而无任何错误 */
    private static final Duration NEO4J_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration NEO4J_READ_TIMEOUT = Duration.ofSeconds(30);

    /** 单次抽取送进 LLM 的最大文本长度 */
    private static final int EXTRACT_MAX_CHARS = 3000;

    /**
     * 抽取结果的最大生成 token 数。
     * <p>必须给推理类模型留足余量：R1 / o1 系（部分中转以 gpt-5.x 等名义提供）会把
     * "思考过程"写进 reasoning_content，而这部分<b>同样消耗 max_tokens</b>。给窄了
     * 思考还没写完额度就耗尽，真正装 JSON 的 content 压根没生成——表现就是
     * "调用成功、不报错，但一条三元组都没有"。</p>
     * <p>上传已异步化，不再受前端 30 秒超时牵制，无需为了提速牺牲输出完整性。</p>
     */
    private static final int EXTRACT_MAX_TOKENS = 4000;

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

    /**
     * 抽取结果：三元组条数 + 失败原因（成功时 reason 为 null）。
     * <p>把诊断信息回传给上层，异步上传场景下用户能直接在前端看到「为什么没抽到」，
     * 不用翻后端日志也能定位是供应商问题、LLM 输出格式问题还是内容问题。</p>
     */
    public record ExtractResult(int triples, String reason) {
    }

    /**
     * 文档发布后抽取三元组入库（失败仅告警，不影响主流程）。
     *
     * @return 抽取结果（条数 + 失败原因）；图谱未启用时返回 0 + 原因
     */
    public ExtractResult extractAndStore(Long docId, String title, String content) {
        if (!properties.isEnabled()) {
            return new ExtractResult(0, "图谱未启用（环境变量 AURORA_NEO4J_ENABLED=true）");
        }
        StringBuilder reason = new StringBuilder();
        try {
            List<Triple> triples = parseTriples(extractTriples(content, reason), reason);
            if (triples.isEmpty()) {
                String r = reason.length() > 0 ? reason.toString() : "模型未生成任何三元组";
                log.warn("图谱抽取：未得到任何三元组 docId={} contentChars={} reason={}",
                        docId, content.length(), r);
                return new ExtractResult(0, r);
            }
            // 一次事务批量写入：UNWIND + 参数列表。
            // 此前是每条三元组一次 HTTP 往返，抽 N 条即 N 次事务提交，是上传超时的次放大器。
            List<Map<String, Object>> rows = new ArrayList<>(triples.size());
            for (Triple t : triples) {
                rows.add(Map.of("s", t.subject(), "o", t.object(),
                        "r", t.relation(), "docId", String.valueOf(docId)));
            }
            runCypher("UNWIND $rows AS row "
                            + "MERGE (s:Entity {name:row.s}) "
                            + "MERGE (o:Entity {name:row.o}) "
                            + "MERGE (s)-[:REL {type:row.r, docId:row.docId}]->(o)",
                    Map.of("rows", rows));
            log.info("Neo4j 图谱抽取完成 docId={} triples={}", docId, triples.size());
            return new ExtractResult(triples.size(), null);
        } catch (Exception e) {
            String msg = "Neo4j 写入异常: " + e.getMessage();
            log.warn("Neo4j 图谱抽取失败 docId={}: {}", docId, e.getMessage());
            return new ExtractResult(0, msg);
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
            throw new IllegalStateException("Neo4j 执行失败: " + errors);
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
                    // 必须显式设超时：默认的 SimpleClientHttpRequestFactory 连接超时为 0（无限等待），
                    // Neo4j 不可达时会把上传请求挂死，前端只看到「超时」而拿不到任何错误。
                    // 用 SimpleClientHttpRequestFactory 而非 JDK HttpClient：后者默认尝试 HTTP/2，
                    // 与 Neo4j 5 的 HTTP 端点不兼容，会报 "Received RST_STREAM: Protocol error"。
                    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                    requestFactory.setConnectTimeout(NEO4J_CONNECT_TIMEOUT);
                    requestFactory.setReadTimeout(NEO4J_READ_TIMEOUT);
                    neo4jClient = RestClient.builder().baseUrl(base)
                            .requestFactory(requestFactory)
                            .defaultHeader("Authorization", "Basic " + auth)
                            .build();
                }
            }
        }
        return neo4jClient;
    }

    private String extractTriples(String content, StringBuilder reason) {
        AiModelProviderDO provider = chatProvider();
        if (provider == null) {
            String msg = "未找到可用供应商（需启用「用途=GRAPH」的供应商，或回退到 CHAT/BOTH 的启用供应商）";
            log.warn("图谱抽取跳过：{}", msg);
            reason.append(msg);
            return "[]";
        }
        String text = content.length() > EXTRACT_MAX_CHARS ? content.substring(0, EXTRACT_MAX_CHARS) : content;
        List<Map<String, Object>> messages = List.of(
                Map.<String, Object>of("role", "system", "content",
                        "你是知识图谱抽取器。从文本中抽取实体关系三元组，仅输出 JSON 数组，"
                                + "格式：[{\"subject\":\"实体1\",\"relation\":\"关系\",\"object\":\"实体2\"}]，不要任何解释。"),
                Map.<String, Object>of("role", "user", "content", text));
        try {
            String raw = openAiClientFactory.chatCompletion(provider, messages, 0.0, EXTRACT_MAX_TOKENS);
            // 返回字符数是判断"抽不到"的第一手线索：0/2（就是 []）说明模型没产出，
            // 几百字符但解析失败说明输出形态不对（见 parseTriples 打的原文）
            log.info("图谱抽取：供应商={} 模型={} 输入字符数={} 返回字符数={}",
                    provider.getName(), provider.getModel(), text.length(), raw == null ? 0 : raw.length());
            if (!StringUtils.hasText(raw) || "[]".equals(raw.trim())) {
                String msg = "供应商=" + provider.getName() + " 模型=" + provider.getModel()
                        + " 返回空数组（输入字符数=" + text.length() + "）";
                log.warn("图谱抽取：模型返回空数组，{}", msg);
                reason.append(msg);
            }
            return raw;
        } catch (Exception e) {
            String msg = "供应商=" + provider.getName() + " 模型=" + provider.getModel() + " err=" + e.getMessage();
            log.warn("图谱抽取调用失败：{}", msg);
            reason.append(msg);
            return "[]";
        }
    }

    private List<Triple> parseTriples(String json, StringBuilder reason) {
        List<Triple> out = new ArrayList<>();
        try {
            int s = json.indexOf('[');
            if (s < 0) {
                String msg = "模型输出中未找到 JSON 数组（原文前 200 字符=" + preview(json) + "）";
                log.warn("图谱抽取：{}", msg);
                reason.append(msg);
                return out;
            }
            int e = json.lastIndexOf(']');
            String candidate;
            if (e > s) {
                candidate = json.substring(s, e + 1);
            } else {
                // 输出被 max_tokens 截断（没有收尾的 ]）：回退到最后一个完整对象再补 ]，
                // 抢救已生成的部分，而不是整批丢弃
                int lastBrace = json.lastIndexOf('}');
                if (lastBrace <= s) {
                    String msg = "模型输出截断且无完整对象（原文前 200 字符=" + preview(json) + "）";
                    log.warn("图谱抽取：{}", msg);
                    reason.append(msg);
                    return out;
                }
                candidate = json.substring(s, lastBrace + 1) + "]";
                String msg = "模型输出疑似被 max_tokens 截断，已抢救截断前的完整三元组";
                log.warn("图谱抽取：{}", msg);
                reason.append(msg);
            }
            JsonNode arr = objectMapper.readTree(candidate);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(new Triple(n.path("subject").asText(), n.path("relation").asText(), n.path("object").asText()));
                }
            }
        } catch (Exception ex) {
            String msg = "三元组 JSON 解析失败（原文前 200 字符=" + preview(json) + "）";
            log.warn("图谱抽取：{} err={}", msg, ex.getMessage());
            reason.append(msg);
        }
        return out;
    }

    /** 截断长文本用于日志，避免模型返回大段内容把日志冲爆 */
    private static String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private List<String> extractEntities(String query) {
        AiModelProviderDO provider = chatProvider();
        if (provider == null) {
            return List.of();
        }
        List<Map<String, Object>> messages = List.of(
                Map.<String, Object>of("role", "system", "content",
                        "从用户问题中抽取涉及的知识实体名称（人物/组织/概念/地点），"
                                + "仅输出 JSON 数组字符串如 [\"实体A\",\"实体B\"]，无实体则输出 []。"),
                Map.<String, Object>of("role", "user", "content", query));
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
        // 优先用「图谱抽取」专用 provider（用途下拉 GRAPH）；未配置则回退聊天 provider，零配置兼容现状
        AiModelProviderDO graph = providerMapper.selectOne(Wrappers.<AiModelProviderDO>lambdaQuery()
                .eq(AiModelProviderDO::getEnabled, 1)
                .eq(AiModelProviderDO::getUsageType, USAGE_GRAPH)
                .orderByAsc(AiModelProviderDO::getSortOrder)
                .last("LIMIT 1"));
        if (graph != null) {
            return graph;
        }
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
            return new GraphInfo("disabled", "基础设施未就绪：Neo4j 未启用，请设置 AURORA_NEO4J_ENABLED=true", 0, 0);
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
            return new GraphInfo("not_ready", "基础设施未就绪：Neo4j 不可达，请检查容器和连接配置", 0, 0);
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
            List<Map<String, Object>> errors = castList(map.get("errors"));
            if (errors != null && !errors.isEmpty()) {
                throw new IllegalStateException("Neo4j 查询失败: " + errors);
            }
            List<Map<String, Object>> results = castList(map.get("results"));
            return results == null ? List.of() : results;
        } catch (Exception e) {
            log.warn("Neo4j 批量查询失败: {}", e.getMessage());
            throw new IllegalStateException("Neo4j 批量查询失败: " + e.getMessage(), e);
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
