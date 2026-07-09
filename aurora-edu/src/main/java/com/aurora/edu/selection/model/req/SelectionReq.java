package com.aurora.edu.selection.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SelectionReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long courseId;
}
