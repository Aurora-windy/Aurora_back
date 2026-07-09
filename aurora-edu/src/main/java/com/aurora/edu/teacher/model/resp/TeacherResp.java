package com.aurora.edu.teacher.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class TeacherResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String teacherNo;
    private String name;
    private Integer title;
    private String phone;
    private String college;
    private Integer status;
    private LocalDateTime createTime;
}
