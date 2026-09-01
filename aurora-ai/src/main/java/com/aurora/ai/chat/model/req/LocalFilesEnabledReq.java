package com.aurora.ai.chat.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 当前会话本地工作区只读能力开关。 */
@Data
public class LocalFilesEnabledReq {
    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
