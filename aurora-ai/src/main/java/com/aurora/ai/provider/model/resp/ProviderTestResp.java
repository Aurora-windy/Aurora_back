package com.aurora.ai.provider.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ProviderTestResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String message;
    private Long durationMs;
}