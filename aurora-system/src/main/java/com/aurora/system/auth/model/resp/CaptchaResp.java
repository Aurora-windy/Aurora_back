package com.aurora.system.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图形验证码响应
 */
@Data
@Builder
@Schema(description = "图形验证码响应")
public class CaptchaResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码标识（登录时回传）", example = "090b9a2c-1691-4fca-99db-e4ed0cff362f")
    private String uuid;

    @Schema(description = "Base64 图片，可直接 <img :src=\"img\">", example = "data:image/png;base64,...")
    private String img;
}
