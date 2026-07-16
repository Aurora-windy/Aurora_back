package com.aurora.ai.knowledge.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunker {

    private static final int TARGET_SIZE = 800;
    private static final int OVERLAP_SIZE = 120;

    public List<String> chunk(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        List<String> paragraphs = splitParagraphs(content);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() > 0 && current.length() + paragraph.length() + 2 > TARGET_SIZE) {
                addChunk(chunks, current.toString());
                current = new StringBuilder(overlap(current.toString()));
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }
        addChunk(chunks, current.toString());
        return chunks;
    }

    private List<String> splitParagraphs(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (StringUtils.hasText(trimmed)) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    private String overlap(String text) {
        String trimmed = text.trim();
        if (trimmed.length() <= OVERLAP_SIZE) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - OVERLAP_SIZE);
    }

    private void addChunk(List<String> chunks, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (StringUtils.hasText(trimmed)) {
            chunks.add(trimmed);
        }
    }
}