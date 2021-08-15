package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(exclude = "workflow",callSuper = false)
@ToString(exclude = "workflow")
public class WorkflowStep extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Workflow workflow;

    @OneToOne(fetch = FetchType.EAGER)
    private IssueStatus issueStatus;

    public WorkflowStep(IssueStatus issueStatus, @NotEmpty(message = "Workflow is required") Workflow workflow) {
        this.issueStatus = issueStatus;
        this.workflow = workflow;
    }
}
