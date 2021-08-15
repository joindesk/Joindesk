package com.ariseontech.joindesk.git.repo;

import com.ariseontech.joindesk.git.domain.GitHook;
import com.ariseontech.joindesk.git.domain.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface HookRepo extends JpaRepository<GitHook, Long> {

    Set<GitHook> findAllByRepository(Repository repository);

}