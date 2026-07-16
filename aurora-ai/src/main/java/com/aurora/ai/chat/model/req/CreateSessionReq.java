package com.aurora.ai.chat.model.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CreateSessionReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String title;
    private Long providerId;
}