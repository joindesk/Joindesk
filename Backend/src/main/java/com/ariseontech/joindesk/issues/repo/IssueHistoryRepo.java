package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface IssueHistoryRepo extends JpaRepository<IssueHistory, Long> {

    Set<IssueHistory> findByIssueOrderByUpdatedDesc(Issue issue);
}