package com.ariseontech.joindesk.issues.domain;

import lombok.Data;

import java.util.List;

@Data
public class FilteredIssueDTO {
    long total, pageSize, pageIndex;
    List<Issue> issues;
}
