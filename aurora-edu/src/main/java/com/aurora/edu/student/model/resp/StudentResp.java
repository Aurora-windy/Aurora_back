package com.aurora.edu.student.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String studentNo;
    private String name;
    private Integer gender;
    private String phone;
    private String major;
    private String className;
    private Integer status;
    private LocalDateTime createTime;
}
