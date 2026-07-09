package com.aurora.edu.course.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseUpdateReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long id;
    @NotBlank
    private String courseCode;
    @NotBlank
    private String name;
    @NotNull
    private Long teacherId;
    private Integer category;
    private BigDecimal credit;
    @NotNull
    private Integer capacity;
    private LocalDateTime selectionStartTime;
    private LocalDateTime selectionEndTime;
    private Integer status;
}
