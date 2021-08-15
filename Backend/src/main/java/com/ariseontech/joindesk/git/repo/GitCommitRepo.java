package com.ariseontech.joindesk.git.repo;

import com.ariseontech.joindesk.git.domain.GitCommit;
import com.ariseontech.joindesk.git.domain.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface GitCommitRepo extends JpaRepository<GitCommit, Long> {

    Set<GitCommit> findByIssuesContainingIgnoreCase(String issueKey);

    Long countByIssuesContainingIgnoreCase(String issueKey);

    GitCommit findByCommitIdAndRepository(String commitId, Repository repository);
}