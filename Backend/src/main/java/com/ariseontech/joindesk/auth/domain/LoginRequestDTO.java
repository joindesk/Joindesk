package com.ariseontech.joindesk.auth.domain;

import com.ariseontech.joindesk.auth.domain.PreferredAuthTypes;
import lombok.Data;

@Data
public class LoginRequestDTO {
    private String username;
    private String password;
    private String token;
    private String confirmPassword;
    private boolean remember;
    private String deviceInfo;
    private String deviceFp;
    private PreferredAuthTypes mode;
}
