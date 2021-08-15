package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueStatusRepo extends JpaRepository<IssueStatus, Long> {

    IssueStatus findByName(String name);

}
