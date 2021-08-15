package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Attachment;
import com.ariseontech.joindesk.issues.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.Set;

public interface AttachmentRepo extends JpaRepository<Attachment, Long> {

    Set<Attachment> findByIssue(Issue issue);

    Attachment findByIssueAndName(Issue issue, String name);

    Attachment findByIssueAndId(Issue issue, Long id);

    Long countByIssue(Issue issue);

    @Transactional
    @Modifying
    void deleteByIssue(Issue issue);

}