package com.aurora.edu.selection.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class SelectionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long studentId;
    private Long courseId;
    private String courseName;
    private Integer status;
    private LocalDateTime selectedTime;
    private LocalDateTime droppedTime;
}
