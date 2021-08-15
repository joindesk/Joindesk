package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.Set;

public interface IssueViewRepo extends JpaRepository<IssueView, Long> {

    Set<IssueView> findFirst10ByIssue(Issue issue);

    Set<IssueView> findFirst10ByWho(Login who);

    @Transactional
    @Modifying
    void deleteByIssueAndWho(Issue issue, Login who);

    @Transactional
    @Modifying
    void deleteByIssue(Issue issue);

}