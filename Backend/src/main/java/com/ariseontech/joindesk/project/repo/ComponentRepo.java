package com.ariseontech.joindesk.project.repo;

import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface ComponentRepo extends JpaRepository<Component, Long> {

    Set<Component> findByName(String name);

    Set<Component> findByProject(Project project);

    Component findByProjectAndId(Project project, Long fieldId);

    @Query(value = "select * from component where project IN (?1)", nativeQuery = true)
    Set<Component> findAllByProject(Set<Long> projects);
}
