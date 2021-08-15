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
@EqualsAndHashCode(exclude = "workflow", callSuper = false)
@ToString(exclude = "workflow")
public class WorkflowTransition extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    private String description;

    @Column(columnDefinition = "boolean default false")
    private boolean fromAll = false;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "from_step")
    private WorkflowStep fromStep;

    @NotNull(message = "To state is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "to_step")
    private WorkflowStep toStep;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Workflow workflow;

    @Column(columnDefinition = "boolean default false")
    private boolean initial = false;

    public WorkflowTransition(@NotEmpty String name, @NotNull(message = "From step is required") WorkflowStep fromStep, @NotNull(message = "To state is required") WorkflowStep toStep, @NotEmpty(message = "Workflow is required") Workflow workflow) {
        this.name = name;
        this.fromStep = fromStep;
        this.toStep = toStep;
        this.workflow = workflow;
        this.initial = false;
    }
}
