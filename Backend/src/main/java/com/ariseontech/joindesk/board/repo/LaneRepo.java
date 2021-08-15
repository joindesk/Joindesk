package com.ariseontech.joindesk.board.repo;

import com.ariseontech.joindesk.board.domain.Board;
import com.ariseontech.joindesk.board.domain.Lane;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface LaneRepo extends JpaRepository<Lane, Long> {
    Set<Lane> findAllByBoardOrderByLaneOrderAsc(Board board);

    int countByBoard(Board board);
}