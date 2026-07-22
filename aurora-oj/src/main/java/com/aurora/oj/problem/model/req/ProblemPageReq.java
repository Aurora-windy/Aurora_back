package com.aurora.oj.problem.model.req;

import com.aurora.common.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OJ problem page request")
public class ProblemPageReq extends PageRequest {

    @Schema(description = "Title keyword")
    private String title;

    @Schema(description = "Difficulty: 1 easy, 2 medium, 3 hard")
    private Integer difficulty;

    @Schema(description = "Status: 0 disabled, 1 enabled")
    private Integer status;
}
