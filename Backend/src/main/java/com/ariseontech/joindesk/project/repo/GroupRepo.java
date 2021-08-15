package com.ariseontech.joindesk.project.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Group;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface GroupRepo extends JpaRepository<Group, Long> {

    Set<Group> findByNameOrderByNameAsc(String name);

    Group findByProjectAndId(Project project, Long id);

    Set<Group> findByProjectOrderByNameAsc(Project project);

    Set<Group> findByUsersOrderByNameAsc(Login l);

    Set<Group> findByAllUsersTrueOrderByNameAsc();

}
