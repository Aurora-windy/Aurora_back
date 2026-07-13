package com.aurora.edu.student.model.req;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StudentAccountOptionReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private String keyword;
    private Long currentStudentId;
    private Integer limit = DEFAULT_LIMIT;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public int normalizedLimit() {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}