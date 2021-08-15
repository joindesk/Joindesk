package com.ariseontech.joindesk.issues.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public
class IssueFilterOptions {
    private String name, value;

    public IssueFilterOptions(String value, String name) {
        this.name = name;
        this.value = value;
    }
}
