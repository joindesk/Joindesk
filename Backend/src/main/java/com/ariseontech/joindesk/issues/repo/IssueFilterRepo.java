package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface IssueFilterRepo extends JpaRepository<IssueFilter, Long> {

    List<IssueFilter> findByOwner(Login login);

    List<IssueFilter> findByOwnerOrOpenTrue(Login login);

    List<IssueFilter> findByOpenTrue();

}