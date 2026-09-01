package com.aurora.ai.workspace;

import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** 受工作区根目录约束的本地文件只读能力。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalWorkspaceService {

    private static final String CAPABILITY = "LOCAL_FILES_READ";
    private static final int MAX_DEPTH = 8;
    private static final List<String> SENSITIVE_NAMES = List.of(
            ".env", ".git", ".ssh", "id_rsa", "id_dsa", "credentials", "secrets");

    private final LocalWorkspaceProperties properties;
    private final AiChatSessionMapper sessionMapper;

    public AiToolResult list(Long sessionId, Map<String, Object> params) {
        AiToolResult access = ensureEnabled(sessionId);
        if (access != null) return access;
        try {
            Path directory = resolve(params == null ? null : stringParam(params, "path"));
            if (!Files.isDirectory(directory)) {
                return fail("LOCAL_PATH_NOT_DIRECTORY", "目标路径不是目录");
            }
            int requestedDepth = intParam(params, "depth", 1);
            int depth = Math.max(0, Math.min(MAX_DEPTH, requestedDepth));
            List<Map<String, Object>> entries = new ArrayList<>();
            try (Stream<Path> stream = Files.list(directory)) {
                stream.sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .filter(this::isSafeEntry)
                        .limit(Math.max(1, properties.getWorkspaceMaxEntries()))
                        .forEach(path -> {
                            if (!isSensitive(path)) {
                                Map<String, Object> item = new LinkedHashMap<>();
                                item.put("name", path.getFileName().toString());
                                item.put("path", relative(path));
                                item.put("type", Files.isDirectory(path) ? "directory" : "file");
                                if (Files.isRegularFile(path)) {
                                    try {
                                        item.put("size", Files.size(path));
                                    } catch (IOException ignored) {
                                        item.put("size", null);
                                    }
                                }
                                if (depth > 0 && Files.isDirectory(path)) {
                                    item.put("hasChildren", hasChildren(path));
                                }
                                entries.add(item);
                            }
                        });
            }
            return AiToolResult.ok(Map.of("root", relative(directory), "entries", entries),
                    "已列出工作区目录");
        } catch (WorkspaceException ex) {
            return fail(ex.code, ex.getMessage());
        } catch (Exception ex) {
            log.warn("local.workspace list failed: {}", ex.getMessage());
            return fail("LOCAL_LIST_FAILED", "目录读取失败");
        }
    }

    public AiToolResult search(Long sessionId, Map<String, Object> params) {
        AiToolResult access = ensureEnabled(sessionId);
        if (access != null) return access;
        String query = params == null ? null : stringParam(params, "query");
        if (!StringUtils.hasText(query)) {
            return fail("LOCAL_SEARCH_QUERY_REQUIRED", "搜索关键词不能为空");
        }
        try {
            Path directory = resolve(params == null ? null : stringParam(params, "path"));
            if (!Files.isDirectory(directory)) {
                return fail("LOCAL_PATH_NOT_DIRECTORY", "目标路径不是目录");
            }
            String needle = query.toLowerCase(Locale.ROOT);
            List<Map<String, Object>> matches = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(directory, MAX_DEPTH)) {
                stream.filter(Files::isRegularFile)
                        .filter(this::isSafeEntry)
                        .filter(path -> !isSensitive(path))
                        .filter(path -> relative(path).toLowerCase(Locale.ROOT).contains(needle))
                        .limit(Math.max(1, properties.getWorkspaceMaxSearchResults()))
                        .forEach(path -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("path", relative(path));
                            try {
                                item.put("size", Files.size(path));
                            } catch (IOException ignored) {
                                item.put("size", null);
                            }
                            matches.add(item);
                        });
            }
            return AiToolResult.ok(Map.of("query", query, "matches", matches), "已完成工作区文件名搜索");
        } catch (WorkspaceException ex) {
            return fail(ex.code, ex.getMessage());
        } catch (Exception ex) {
            log.warn("local.workspace search failed: {}", ex.getMessage());
            return fail("LOCAL_SEARCH_FAILED", "文件搜索失败");
        }
    }

    public AiToolResult read(Long sessionId, Map<String, Object> params) {
        AiToolResult access = ensureEnabled(sessionId);
        if (access != null) return access;
        String rawPath = params == null ? null : stringParam(params, "path");
        if (!StringUtils.hasText(rawPath)) {
            return fail("LOCAL_READ_PATH_REQUIRED", "文件路径不能为空");
        }
        try {
            Path file = resolve(rawPath);
            if (!Files.isRegularFile(file)) {
                return fail("LOCAL_PATH_NOT_FILE", "目标路径不是文件");
            }
            if (isSensitive(file)) {
                return fail("LOCAL_SENSITIVE_FILE", "出于安全原因，不允许读取该文件");
            }
            int maxChars = intParam(params, "maxChars", properties.getWorkspaceMaxReadChars());
            maxChars = Math.max(1, Math.min(properties.getWorkspaceMaxReadChars(), maxChars));
            int maxBytes = Math.max(4, maxChars * 4 + 1);
            byte[] bytes;
            try (InputStream input = Files.newInputStream(file)) {
                bytes = input.readNBytes(maxBytes);
            }
            if (isBinary(bytes)) {
                return fail("LOCAL_BINARY_FILE", "不允许读取二进制文件");
            }
            String content = new String(bytes, StandardCharsets.UTF_8);
            boolean truncated = content.length() > maxChars;
            if (truncated) {
                content = content.substring(0, maxChars);
            }
            return AiToolResult.ok(Map.of("path", relative(file), "content", content, "truncated", truncated),
                    "已读取工作区文件");
        } catch (WorkspaceException ex) {
            return fail(ex.code, ex.getMessage());
        } catch (Exception ex) {
            log.warn("local.workspace read failed path={} error={}", rawPath, ex.getMessage());
            return fail("LOCAL_READ_FAILED", "文件读取失败");
        }
    }

    private Path resolve(String rawPath) throws IOException {
        if (!StringUtils.hasText(properties.getWorkspaceRoot())) {
            throw new WorkspaceException("LOCAL_WORKSPACE_DISABLED", "服务端未配置本地工作区");
        }
        Path root = workspaceRoot();
        if (!Files.isDirectory(root)) {
            throw new WorkspaceException("LOCAL_WORKSPACE_INVALID", "本地工作区目录不可用");
        }
        String requested = StringUtils.hasText(rawPath) ? rawPath : ".";
        Path input = Path.of(requested);
        if (input.isAbsolute()) {
            throw new WorkspaceException("LOCAL_PATH_OUTSIDE_WORKSPACE", "只允许访问工作区相对路径");
        }
        Path candidate = root.resolve(input).normalize();
        if (!candidate.startsWith(root) || !Files.exists(candidate)) {
            throw new WorkspaceException("LOCAL_PATH_OUTSIDE_WORKSPACE", "路径不存在或超出工作区范围");
        }
        Path real = candidate.toRealPath();
        if (!real.startsWith(root)) {
            throw new WorkspaceException("LOCAL_PATH_OUTSIDE_WORKSPACE", "路径不存在或超出工作区范围");
        }
        if (isSensitive(real)) {
            throw new WorkspaceException("LOCAL_SENSITIVE_FILE", "出于安全原因，不允许访问该路径");
        }
        return real;
    }

    /** 目录枚举/搜索也必须解析真实路径，避免通过工作区内 symlink 观察外部文件。 */
    private boolean isSafeEntry(Path path) {
        try {
            Path real = path.toRealPath();
            return real.startsWith(workspaceRoot()) && !isSensitive(real);
        } catch (IOException ex) {
            return false;
        }
    }

    private Path workspaceRoot() throws IOException {
        if (!StringUtils.hasText(properties.getWorkspaceRoot())) {
            throw new WorkspaceException("LOCAL_WORKSPACE_DISABLED", "服务端未配置本地工作区");
        }
        Path root = Path.of(properties.getWorkspaceRoot()).toAbsolutePath().normalize().toRealPath();
        if (!Files.isDirectory(root)) {
            throw new WorkspaceException("LOCAL_WORKSPACE_INVALID", "本地工作区目录不可用");
        }
        return root;
    }

    private boolean isSensitive(Path path) {
        for (Path part : path) {
            String name = part.toString().toLowerCase(Locale.ROOT);
            if (SENSITIVE_NAMES.contains(name) || name.endsWith(".pem") || name.endsWith(".key")
                    || name.endsWith(".p12") || name.endsWith(".jks")) {
                return true;
            }
        }
        return false;
    }

    /** 拒绝 NUL、非法 UTF-8 以及控制字符占比过高的内容，避免把二进制喂给模型。 */
    private boolean isBinary(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            String text = decoder.decode(ByteBuffer.wrap(bytes)).toString();
            long controls = text.chars()
                    .filter(codePoint -> Character.isISOControl(codePoint)
                            && codePoint != '\n' && codePoint != '\r' && codePoint != '\t')
                    .count();
            return !text.isEmpty() && controls * 100 > text.length() * 2;
        } catch (CharacterCodingException ex) {
            return true;
        }
    }

    private boolean hasChildren(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.findAny().isPresent();
        } catch (IOException ex) {
            return false;
        }
    }

    private String relative(Path path) {
        try {
            Path root = workspaceRoot();
            return root.relativize(path).toString().replace('\\', '/');
        } catch (IOException ex) {
            return path.toString().replace('\\', '/');
        }
    }

    private String stringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private int intParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(params.get(key)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private AiToolResult fail(String code, String message) {
        return AiToolResult.fail(code, message);
    }

    private AiToolResult ensureEnabled(Long sessionId) {
        AiChatSessionDO session = sessionId == null ? null : sessionMapper.selectById(sessionId);
        if (session == null || !Integer.valueOf(1).equals(session.getLocalFilesEnabled())) {
            return fail("LOCAL_FILES_DISABLED", "当前会话未开启本地工作区工具");
        }
        return null;
    }

    private static final class WorkspaceException extends RuntimeException {
        private final String code;

        private WorkspaceException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
