package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Workflow;
import com.ariseontech.joindesk.issues.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface WorkflowTransitionRepo extends JpaRepository<WorkflowTransition, Long> {

    WorkflowTransition findByName(String name);

    Set<WorkflowTransition> findByWorkflow(Workflow workflow);

}
