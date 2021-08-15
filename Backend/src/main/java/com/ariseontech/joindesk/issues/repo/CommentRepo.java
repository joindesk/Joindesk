package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Comment;
import com.ariseontech.joindesk.issues.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface CommentRepo extends JpaRepository<Comment, Long> {

    Set<Comment> findByIssueOrderByCreatedDateDesc(Issue issue);
}