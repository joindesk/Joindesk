package com.ariseontech.joindesk.git.repo;

import com.ariseontech.joindesk.git.domain.Repository;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface RepositoryRepo extends JpaRepository<Repository, Long> {

    Set<Repository> findAllByProject(Project project);

    Repository findByUuid(String uuid);

}