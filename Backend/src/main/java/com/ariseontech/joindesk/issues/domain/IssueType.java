package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.project.domain.Project;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@NoArgsConstructor
@Data
public class IssueType extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    private String iconUrl;

    private String description;

    @NotNull(message = "Project is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @PropertyView("issue_type_detail")
    private Project project;

    @NotNull(message = "Workflow is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow")
    @PropertyView("issue_type_detail")
    private Workflow workflow;

    @PropertyView("issue_type_detail")
    private Date createdDate;

    @Transient
    @PropertyView("issue_type_detail")
    private boolean editable = false;

    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    public IssueType(@NotEmpty String name, String iconUrl, String description, Project project) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.description = description;
        this.project = project;
        this.createdDate = new Date();
    }
}
