package com.ariseontech.joindesk.project.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.GlobalGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface GlobalGroupRepo extends JpaRepository<GlobalGroup, Long> {

    Set<GlobalGroup> findByNameOrderByNameAsc(String name);

    Set<GlobalGroup> findByUsersOrderByNameAsc(Login l);

}
