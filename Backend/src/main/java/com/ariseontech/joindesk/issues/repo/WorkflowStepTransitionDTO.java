package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.WorkflowStep;
import com.ariseontech.joindesk.issues.domain.WorkflowTransition;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WorkflowStepTransitionDTO {

    private WorkflowStep step;

    private List<WorkflowTransition> workflowTransitions;

}
