package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueOtherRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface IssueOtherRelationshipRepo extends JpaRepository<IssueOtherRelationship, Long> {
    Set<IssueOtherRelationship> findByIssue(Issue issue);
}