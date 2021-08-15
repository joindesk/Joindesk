package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueMentions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface IssueMentionsRepo extends JpaRepository<IssueMentions, Long> {

    Set<IssueMentions> findByLinkTrue();

    Set<IssueMentions> findByMentionTrue();

    Set<IssueMentions> findByIssue(Issue issue);

}