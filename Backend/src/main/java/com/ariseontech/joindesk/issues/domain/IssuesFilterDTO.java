package com.ariseontech.joindesk.issues.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class IssuesFilterDTO {

    public long pageSize, pageIndex;

    public String containsText;

    public Map<String, IssueFilterGroup> filterOptions;

    public Set<String> selected;

    public String timezone;

    public IssueFilter filter;

    public Set<String> sortOptions;

    public IssuesFilterDTO() {
        sortOptions = Set.of("Created", "Updated", "Due", "Assignee", "Reporter", "Key", "Priority");
        pageSize = 10;
        pageIndex = 0;
        // TODO
        //resolutions.add(new Resolution("unresolved"));
        // TODO
        //assignee.add(new Login(0L, "unassigned", "", "unassigned"));
        selected = new HashSet<>();
        selected.add("resolution");
        selected.add("assignee");
        filterOptions = new HashMap<>();
    }
}

