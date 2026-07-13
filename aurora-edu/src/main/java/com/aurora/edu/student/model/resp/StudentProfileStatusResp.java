package com.aurora.edu.student.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class StudentProfileStatusResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean bound;
    private boolean enabled;
    private Long studentId;
    private String message;
}