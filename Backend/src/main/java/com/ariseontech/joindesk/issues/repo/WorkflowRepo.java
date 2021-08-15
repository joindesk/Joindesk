package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Workflow;
import com.ariseontech.joindesk.project.domain.Project;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRepo extends JpaRepository<Workflow, Long> {

    Workflow findByName(String name);

    List<Workflow> findByProject(Project project);

}
