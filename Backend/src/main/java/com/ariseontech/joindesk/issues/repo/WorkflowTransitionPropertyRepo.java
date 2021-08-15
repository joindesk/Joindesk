package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.WorkflowTransition;
import com.ariseontech.joindesk.issues.domain.WorkflowTransitionProperties;
import com.ariseontech.joindesk.issues.domain.WorkflowTransitionPropertySubTypes;
import com.ariseontech.joindesk.issues.domain.WorkflowTransitionPropertyTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface WorkflowTransitionPropertyRepo extends JpaRepository<WorkflowTransitionProperties, Long> {

    Set<WorkflowTransitionProperties> findAllByTransition(WorkflowTransition transition);

    Set<WorkflowTransitionProperties> findAllByTransitionAndType(WorkflowTransition transition, WorkflowTransitionPropertyTypes workflowTransitionPropertyTypes);

    Set<WorkflowTransitionProperties> findAllByTransitionAndTypeAndSubType(WorkflowTransition transition,
                                                                           WorkflowTransitionPropertyTypes workflowTransitionPropertyTypes,
                                                                           WorkflowTransitionPropertySubTypes workflowTransitionPropertySubTypes);

    Set<WorkflowTransitionProperties> findAllByTransitionAndSubType(WorkflowTransition transition,
                                                                    WorkflowTransitionPropertySubTypes workflowTransitionPropertySubTypes);

}
