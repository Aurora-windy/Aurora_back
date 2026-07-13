package com.aurora.edu.student.model.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StudentAccountOptionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
}