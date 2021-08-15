package com.ariseontech.joindesk.auth.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {
    @Autowired
    private CurrentLogin currentLogin;

    public Optional<String> getCurrentAuditor() {
        if (currentLogin != null && currentLogin.getUser() != null)
            return Optional.ofNullable(currentLogin.getUser().getUserName());
        else return Optional.empty();
    }
}
