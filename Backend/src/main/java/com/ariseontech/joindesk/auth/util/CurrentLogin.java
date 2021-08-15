package com.ariseontech.joindesk.auth.util;

import com.ariseontech.joindesk.auth.domain.AuthorityGlobal;
import com.ariseontech.joindesk.auth.domain.AuthorityProject;
import com.ariseontech.joindesk.auth.domain.Login;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Data
public class CurrentLogin {

    // Actual logged in user
    private Login user;

    // API token for Temp storage
    @JsonIgnore
    private String token;

    private List<AuthorityProject> authorities;

    private List<AuthorityGlobal> authoritiesGlobal;

    public static String trimDoubleQuotes(String text) {
        int textLength = text.length();

        if (textLength >= 2 && text.charAt(0) == '"' && text.charAt(textLength - 1) == '"') {
            return text.substring(1, textLength - 1);
        }

        return text.replaceAll("\\\\", "");
    }

}
