package com.ariseontech.joindesk.auth.domain;

import lombok.Data;

@Data
public class ChangePasswordDTO {
    String currentPassword, newPassword, confirmNewPassword;
}