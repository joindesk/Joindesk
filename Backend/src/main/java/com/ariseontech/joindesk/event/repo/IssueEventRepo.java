package com.ariseontech.joindesk.event.repo;

import com.ariseontech.joindesk.event.domain.IssueEvent;
import com.ariseontech.joindesk.issues.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;

public interface IssueEventRepo extends JpaRepository<IssueEvent, Long> {
    @Transactional
    @Modifying
    void deleteByIssue(Issue issue);
}