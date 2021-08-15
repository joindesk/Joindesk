package com.ariseontech.joindesk.board.repo;

import com.ariseontech.joindesk.board.domain.Board;
import com.ariseontech.joindesk.issues.domain.IssueFilter;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface BoardRepo extends JpaRepository<Board, Long> {

    Set<Board> findAllByProject(Project project);

    Set<Board> findAllByFilter(IssueFilter filter);

    Set<Board> findAllByProjectAndActiveTrue(Project project);
}