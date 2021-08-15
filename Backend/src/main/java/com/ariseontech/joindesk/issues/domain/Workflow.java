package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.issues.repo.WorkflowStepTransitionDTO;
import com.ariseontech.joindesk.project.domain.Project;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Table(indexes = {
        @Index(name = "JD_WF_IDX", columnList = "project")
})
public class Workflow extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    @PropertyView("workflow_detail")
    private String description;

    @NotNull(message = "Project is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "project")
    @PropertyView("workflow_detail")
    private Project project;

    @Transient
    @PropertyView("workflow_detail")
    private Set<WorkflowStep> workflowSteps;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkflowStep defaultStep;

    @Transient
    @PropertyView("workflow_detail")
    private Set<WorkflowTransition> workflowTransitions;

    @PropertyView("workflow_detail")
    private Date createdDate;

    @Transient
    @PropertyView("workflow_detail")
    private boolean editable = false;

    @Transient
    @PropertyView("workflow_detail")
    private List<WorkflowStepTransitionDTO> workflowStepTransitions;

    public Workflow(@NotEmpty String name, @NotNull(message = "Project is required") Project project) {
        this.name = name;
        this.project = project;
        createdDate = new Date();
    }
}
