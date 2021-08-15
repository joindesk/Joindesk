package com.ariseontech.joindesk.issues.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class WorkflowChange {

    Map<WorkflowStep, WorkflowStep> map;

    Set<WorkflowStep> fromSteps;

    Set<WorkflowStep> toSteps;

    public WorkflowChange() {
        this.map = new HashMap<>();
        this.fromSteps = new HashSet<>();
        this.toSteps = new HashSet<>();
    }
}
