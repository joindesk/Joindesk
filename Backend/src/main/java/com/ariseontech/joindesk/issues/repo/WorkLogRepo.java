package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.WorkLog;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.Set;

public interface WorkLogRepo extends JpaRepository<WorkLog, Long> {

    Set<WorkLog> findByIssue(Issue issue);

    @Query(value = "select sum(work_minutes) from work_log where issue = ?1", nativeQuery = true)
    Long sumOfWorkLoggedByIssue(Issue issue);

    Set<WorkLog> findByProject(Project project);

    Set<WorkLog> findByCreatedBy(Login createdBy);

    Set<WorkLog> findByByAndWorkFromBetween(Login by, Timestamp from, Timestamp to);

    Set<WorkLog> findByProjectAndWorkFromBetween(Project project, Timestamp from, Timestamp to);

}
