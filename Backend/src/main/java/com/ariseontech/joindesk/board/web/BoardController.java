package com.ariseontech.joindesk.board.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.board.domain.Board;
import com.ariseontech.joindesk.board.domain.Lane;
import com.ariseontech.joindesk.board.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = "application/json", consumes = "application/json",value = SystemInfo.apiPrefix)
public class BoardController {

    @Autowired
    private BoardService boardService;

    @GetMapping("project/{project_key}/board")
    public String getBoards(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base,-user_detail", boardService.getAllForProject(projectKey));
    }

    @GetMapping("project/{project_key}/board/{board_id}")
    public String getBoard(@PathVariable("project_key") String projectKey, @PathVariable("board_id") Long boardID) {
        return HelperUtil.squiggly("base,-user_detail", boardService.getBoard(boardID));
    }

    @PostMapping("project/{project_key}/board/")
    public String saveBoard(@PathVariable("project_key") String projectKey, @RequestBody Board board) {
        return HelperUtil.squiggly("base,-user_detail", boardService.saveBoard(projectKey, board));
    }

    @PostMapping("project/{project_key}/board/remove")
    public void deleteBoard(@PathVariable("project_key") String projectKey, @RequestBody Board board) {
        boardService.removeBoard(projectKey, board);
    }

    @GetMapping("project/{project_key}/board/{board_id}/lane/{lane_id}")
    public String getLane(@PathVariable("lane_id") Long laneID) {
        return HelperUtil.squiggly("base,-user_detail", boardService.getLane(laneID));
    }

    @PostMapping("project/{project_key}/board/{board_id}/lane/")
    public String saveLane(@PathVariable("board_id") Long boardID, @RequestBody Lane lane) {
        return HelperUtil.squiggly("base,-user_detail", boardService.saveLane(boardID, lane));
    }

    @PostMapping("project/{project_key}/board/{board_id}/lane/remove")
    public void deleteLane(@PathVariable("board_id") Long boardID, @RequestBody Lane lane) {
        boardService.removeLane(boardID, lane);
    }


}
