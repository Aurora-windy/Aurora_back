package com.aurora.ai.knowledge.support;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 零依赖文档文本提取器。
 * <p>支持 .txt / .md / .markdown / .csv（UTF-8 直接读）与 .docx / .xlsx
 * （docx/xlsx 本质是 zip+xml，用 JDK 自带 {@link ZipInputStream} + 正则提取文本，
 * 不引入 POI/PDFBox 等外部依赖，离线可编译）。</p>
 */
@Slf4j
public final class DocTextExtractor {

    private DocTextExtractor() {
    }

    private static final Pattern PARA_PATTERN = Pattern.compile("<w:p[ >].*?</w:p>", Pattern.DOTALL);
    private static final Pattern TEXT_PATTERN = Pattern.compile("<w:t[^>]*>(.*?)</w:t>", Pattern.DOTALL);
    private static final Pattern SHARED_TEXT_PATTERN = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL);
    private static final Pattern ROW_PATTERN = Pattern.compile("<row[ >].*?</row>", Pattern.DOTALL);
    private static final Pattern CELL_TEXT_PATTERN = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL);

    /**
     * 按扩展名提取纯文本。
     *
     * @throws IllegalArgumentException 不支持的扩展名或文件内容不合法
     */
    public static String extract(String fileName, byte[] bytes) throws Exception {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) {
            return extractDocx(bytes);
        }
        if (lower.endsWith(".xlsx")) {
            return extractXlsx(bytes);
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".csv")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("不支持的文件类型：" + fileName + "（支持 txt/md/csv/docx/xlsx）");
    }

    /** 是否为受支持的文件扩展名 */
    public static boolean supports(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")
                || lower.endsWith(".csv") || lower.endsWith(".docx") || lower.endsWith(".xlsx");
    }

    /** docx：读取 word/document.xml，按段落提取 <w:t> 文本 */
    private static String extractDocx(byte[] bytes) throws Exception {
        String xml = readZipEntry(bytes, "word/document.xml");
        if (xml == null || xml.isEmpty()) {
            throw new IllegalArgumentException("无法解析 docx（缺少 document.xml）");
        }
        StringBuilder sb = new StringBuilder();
        Matcher paraMatcher = PARA_PATTERN.matcher(xml);
        while (paraMatcher.find()) {
            String para = paraMatcher.group();
            Matcher textMatcher = TEXT_PATTERN.matcher(para);
            StringBuilder line = new StringBuilder();
            while (textMatcher.find()) {
                line.append(textMatcher.group(1));
            }
            if (line.length() > 0) {
                sb.append(line).append('\n');
            }
        }
        if (sb.length() == 0) {
            throw new IllegalArgumentException("docx 中未提取到文本（可能是空白文档）");
        }
        return sb.toString();
    }

    /** xlsx：提取 sharedStrings.xml 的全部文本 + 各 sheet 单元格文本 */
    private static String extractXlsx(byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        // 共享字符串（绝大多数中文/文本内容都在这里）
        String shared = readZipEntry(bytes, "xl/sharedStrings.xml");
        if (shared != null && !shared.isEmpty()) {
            Matcher sharedMatcher = SHARED_TEXT_PATTERN.matcher(shared);
            while (sharedMatcher.find()) {
                sb.append(sharedMatcher.group(1)).append('\n');
            }
        }
        // 工作表内联字符串（数字/公式结果会被忽略，仅补文本）
        String sheetXml = readZipEntry(bytes, "xl/worksheets/sheet1.xml");
        if (sheetXml != null && !sheetXml.isEmpty()) {
            Matcher rowMatcher = ROW_PATTERN.matcher(sheetXml);
            while (rowMatcher.find()) {
                Matcher cellTextMatcher = CELL_TEXT_PATTERN.matcher(rowMatcher.group());
                StringBuilder line = new StringBuilder();
                while (cellTextMatcher.find()) {
                    line.append(cellTextMatcher.group(1)).append('\t');
                }
                if (line.length() > 0) {
                    sb.append(line, 0, line.length() - 1).append('\n');
                }
            }
        }
        if (sb.length() == 0) {
            throw new IllegalArgumentException("xlsx 中未提取到文本（可能是空白表格）");
        }
        return sb.toString();
    }

    /** 从 zip 中读取指定条目文本（UTF-8） */
    private static String readZipEntry(byte[] bytes, String entryName) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    return out.toString(StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
