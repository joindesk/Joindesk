package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueType;
import com.ariseontech.joindesk.issues.domain.Workflow;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface IssueTypeRepo extends JpaRepository<IssueType, Long> {

    Set<IssueType> findByProjectOrderByNameAsc(Project project);

    @Query(value = "select * from issue_type where project IN (?1)", nativeQuery = true)
    Set<IssueType> findAllByProject(Set<Long> projects);

    IssueType findByNameAndProject(String name, Project project);

    Set<IssueType> findByIconUrlAndProject(String iconUrl, Project project);

    List<Issue> findByWorkflowOrderByNameAsc(Workflow workflow);

}
