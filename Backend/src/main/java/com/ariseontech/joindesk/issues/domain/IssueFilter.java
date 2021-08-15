package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Project;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class IssueFilter extends AuditModel {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "query")
    IssueSearchQuery query = new IssueSearchQuery();
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50)
    private String name;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private Login owner;
    private String sortBy = "Updated", sortOrder = "DESC";
    private String searchQuery = "";

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "project")
    private Project project;

    private boolean open;
    @Transient
    private boolean readonly = false;

    public IssueFilter() {
    }

    public IssueFilter(IssueSearchQuery query, Long id, String name, Login owner, String sortBy, String sortOrder, String searchQuery,
                       Project project, boolean open, boolean readonly) {
        this.query = query;
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.searchQuery = searchQuery;
        this.open = open;
        this.project = project;
        this.readonly = readonly;
    }

    public IssueFilter(String name, Login owner, String sortBy, String sortOrder, IssueSearchQuery query, boolean open, Project project) {
        this.name = name;
        this.owner = owner;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.query = query;
        this.project = project;
        this.open = open;
    }
}
