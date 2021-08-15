package com.ariseontech.joindesk.auth.domain;

import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    String user;
    String otp;
    String password, passwordc;
    int stage;
}
