package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueRelationship;
import com.ariseontech.joindesk.issues.domain.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface IssueRelationshipRepo extends JpaRepository<IssueRelationship, Long> {

    Set<IssueRelationship> findByFromIssueOrToIssue(Issue fromIssue, Issue toIssue);

    Set<IssueRelationship> findByTypeAndFromIssueAndToIssue(Relationship type, Issue fromIssue, Issue toIssue);

    Set<IssueRelationship> findByType(Relationship relationship);
}