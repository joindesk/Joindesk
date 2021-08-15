package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TaskRepo extends JpaRepository<Task, Long> {
    Set<Task> findAllByIssue(Issue issue);

    long countAllByIssue(Issue issue);

    void deleteAllByIssue(Issue issue);

    long countAllByIssueAndCompletedFalse(Issue issue);

    @Modifying
    @Query(value = "update task set task_order = ?2 where id = ?1", nativeQuery = true)
    void changeTaskOrder(Long id, Long order);
}