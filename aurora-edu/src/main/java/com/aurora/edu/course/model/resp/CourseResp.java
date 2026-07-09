package com.aurora.edu.course.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CourseResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String courseCode;
    private String name;
    private Long teacherId;
    private Integer category;
    private BigDecimal credit;
    private Integer capacity;
    private Integer selectedCount;
    private Integer remainingCount;
    private LocalDateTime selectionStartTime;
    private LocalDateTime selectionEndTime;
    private Integer status;
    private LocalDateTime createTime;
}
