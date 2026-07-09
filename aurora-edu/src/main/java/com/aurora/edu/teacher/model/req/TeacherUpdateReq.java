package com.aurora.edu.teacher.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeacherUpdateReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long id;
    private Long userId;
    @NotBlank
    private String teacherNo;
    @NotBlank
    private String name;
    private Integer title;
    private String phone;
    private String college;
    private Integer status;
}
