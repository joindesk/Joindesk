package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Workflow;
import com.ariseontech.joindesk.issues.domain.WorkflowStep;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface WorkflowStepRepo extends JpaRepository<WorkflowStep, Long> {

    Set<WorkflowStep> findByWorkflow(Workflow workflow);

    Set<WorkflowStep> findByWorkflowProject(Project project);

}
