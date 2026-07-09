package com.aurora.edu.student.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StudentUpdateReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long id;
    private Long userId;
    @NotBlank
    private String studentNo;
    @NotBlank
    private String name;
    private Integer gender;
    private String phone;
    private String major;
    private String className;
    private Integer status;
}
