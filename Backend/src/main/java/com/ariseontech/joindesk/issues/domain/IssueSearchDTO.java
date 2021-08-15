package com.ariseontech.joindesk.issues.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class IssueSearchDTO {
    @JsonIgnore
    String projectKey;
    String sortBy, sortDir, timezone;
    IssueFilter filter;
    String jql;
    int pageIndex, pageSize;
    List<Issue> issues;
    List<Object> issueKeys;
    long total;

    public IssueSearchDTO(String projectKey, String sortBy, String sortDir, String timezone, IssueFilter filter, int pageIndex, int pageSize) {
        this.projectKey = projectKey;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
        this.timezone = timezone;
        this.filter = filter;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    public IssueSearchDTO() {
        total = 0;
    }
}
