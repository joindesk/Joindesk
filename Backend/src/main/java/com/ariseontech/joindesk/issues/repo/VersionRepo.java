package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Version;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface VersionRepo extends JpaRepository<Version, Long> {

    Set<Version> findByProject(Project project);

    Version findByProjectAndId(Project project, Long id);

}
