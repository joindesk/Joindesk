package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.domain.Project;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
public class IssueFilterDTO {

    public long pageSize, pageIndex;

    public String containsText;

    public List<IssueFilterGroup> filters;

    public Set<String> selected;

    public Set<IssueType> issueType;

    public Set<Resolution> resolutions;

    public List<Priority> priority;

    public Set<Login> assignee, reporter;

    public Set<Project> projects;

    public Set<IssueStatus> status;

    public Version versions;

    public Date createdBefore, createdAfter, updatedBefore, updatedAfter, dueBefore, dueAfter;

    public String timezone;

    public IssueFilter filter;

    // Possible Data
    public Set<String> possibleSorts = new TreeSet<>();

    public Set<IssueType> possibleIssueTypes = new TreeSet<>();

    public Set<Resolution> possibleResolutions = new TreeSet<>();

    public List<Priority> possiblePriorities = Arrays.asList(Priority.values());

    public Set<Project> possibleProjects;

    public Set<Login> possibleMembers;

    public List<IssueStatus> possibleStatus;

    public List<Version> possibleVersions;

    public List<Label> possibleLabels;

    public Set<Component> possibleComponents;

    public IssueFilterDTO() {
        possibleSorts.addAll(Arrays.asList("Created", "Updated", "Due", "Assignee", "Reporter", "Key", "Priority"));
        pageSize = 10;
        pageIndex = 0;
        projects = new HashSet<>();
        possibleMembers = new HashSet<>();
        resolutions = new HashSet<>();
        resolutions.add(new Resolution("unresolved"));
        assignee = new HashSet<>();
        assignee.add(new Login(0L, "unassigned", "", "unassigned"));
        selected = new HashSet<>();
        selected.add("resolution");
        selected.add("assignee");
        filters = new ArrayList<>();
    }
}

