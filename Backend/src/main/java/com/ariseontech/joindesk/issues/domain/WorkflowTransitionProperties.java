package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(indexes = {
        @Index(name = "JD_WFTRANSP_IDX", columnList = "transition")
})
public class WorkflowTransitionProperties extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private WorkflowTransitionPropertyTypes type;

    @NotNull
    @Enumerated(EnumType.STRING)
    private WorkflowTransitionPropertySubTypes subType;

    private String key;

    private String value;

    private String condition = "OR";

    @Transient
    private String displayValue;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "transition", nullable = false)
    @JsonIgnore
    private WorkflowTransition transition;

    @Transient
    private String fromStep, toStep, transitionName;

    public WorkflowTransitionProperties() {
    }

    public WorkflowTransitionProperties(@NotNull WorkflowTransitionPropertyTypes type, @NotNull WorkflowTransitionPropertySubTypes subType, String key, String value, @NotNull(message = "Transition is required") WorkflowTransition transition) {
        this.type = type;
        this.subType = subType;
        this.key = key;
        this.value = value;
        this.transition = transition;
    }
}
