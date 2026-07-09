package com.aurora.edu.student.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StudentAddReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    @NotBlank
    private String studentNo;
    @NotBlank
    private String name;
    private Integer gender;
    private String phone;
    private String major;
    private String className;
    private Integer status = 1;
}
