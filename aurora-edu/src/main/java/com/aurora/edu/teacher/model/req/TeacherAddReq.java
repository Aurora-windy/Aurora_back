package com.aurora.edu.teacher.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeacherAddReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    @NotBlank
    private String teacherNo;
    @NotBlank
    private String name;
    private Integer title = 3;
    private String phone;
    private String college;
    private Integer status = 1;
}
