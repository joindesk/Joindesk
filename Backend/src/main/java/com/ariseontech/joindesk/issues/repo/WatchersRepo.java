package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.Watchers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.Set;

public interface WatchersRepo extends JpaRepository<Watchers, Long> {

    Set<Watchers> findByIssue(Issue issue);

    Set<Watchers> findByWatcher(Login watcher);

    Set<Watchers> findByIssueAndWatcher(Issue issue, Login watcher);

    @Transactional
    @Modifying
    void deleteByIssue(Issue issue);

}
