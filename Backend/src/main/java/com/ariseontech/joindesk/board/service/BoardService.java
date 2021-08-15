package com.ariseontech.joindesk.board.service;

import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.board.domain.Board;
import com.ariseontech.joindesk.board.domain.Lane;
import com.ariseontech.joindesk.board.repo.BoardRepo;
import com.ariseontech.joindesk.board.repo.LaneRepo;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.IssueFilter;
import com.ariseontech.joindesk.issues.domain.IssueSearchDTO;
import com.ariseontech.joindesk.issues.domain.IssueStatus;
import com.ariseontech.joindesk.issues.domain.WorkflowStep;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.issues.service.IssueTypeService;
import com.ariseontech.joindesk.issues.service.WorkflowService;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoardService {

    @Autowired
    private BoardRepo boardRepo;
    @Autowired
    private LaneRepo laneRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private IssueTypeService issueTypeService;
    @Autowired
    private IssueService issueService;
    @Autowired
    private CurrentLogin currentLogin;
    @Autowired
    private WorkflowService workflowService;

    public Set<Board> getAllForProject(String projectKey) {
        Project project = projectService.findByKey(projectKey);
        checkViewAccess(project);
        if (hasManageAccess(project)) {
            return boardRepo.findAllByProject(project);
        }
        return boardRepo.findAllByProjectAndActiveTrue(project);
    }

    public Set<Board> getByFilter(IssueFilter filter) {
        return boardRepo.findAllByFilter(filter);
    }

    public Board getBoard(Long boardId) {
        Optional<Board> b = boardRepo.findById(boardId);
        if (b.isEmpty()) throw new JDException("Board not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Board board = b.get();
        if (!board.isActive() && !hasManageAccess(board.getProject()))
            throw new JDException("Board not active", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        checkViewAccess(board.getProject());
        List<Lane> lanes = new ArrayList<>();
//        List<IssueStatus> allStatuses = issueTypeService.getAllStatus();

        //Filter
        IssueSearchDTO issueSearchDTO = new IssueSearchDTO();
        issueSearchDTO.setPageIndex(1);
        issueSearchDTO.setPageSize(100000000);
        issueSearchDTO.setFilter(board.getFilter());
        issueSearchDTO.setProjectKey(board.getProject().getKey());
        issueSearchDTO.setTimezone(currentLogin.getUser().getTimezone().getID());
        IssueSearchDTO issues = issueService.searchIssuesLucene(board.getProject().getKey(), issueSearchDTO, "");
        List<IssueStatus> allStatuses = workflowService.getAllWorkflowStepsByProject(board.getProject()).stream()
                .map(WorkflowStep::getIssueStatus).collect(Collectors.toList());
        laneRepo.findAllByBoardOrderByLaneOrderAsc(board).forEach(l -> {
            List<IssueStatus> allStatusesCopy = new ArrayList<>(allStatuses);
            laneRepo.findAllByBoardOrderByLaneOrderAsc(board).stream().filter(l2 -> !l.getId().equals(l2.getId())).forEach(ll -> allStatusesCopy.removeAll(ll.getStatuses()));
            l.setPossibleStatuses(new HashSet<>(allStatusesCopy));
            l.setIssues(issues.getIssues().stream()
                    .filter(i -> l.getStatuses().contains(i.getCurrentStep().getIssueStatus())).collect(Collectors.toSet()));
            lanes.add(l);
        });
        board.setLanes(lanes);
        return board;
    }

    public Board saveBoard(String projectKey, Board b) {
        Project project = projectService.findByKey(projectKey);
        Board board;
        if (b.getId() == null) {
            board = new Board();
            board.setProject(project);
        } else {
            Optional<Board> bb = boardRepo.findById(b.getId());
            if (bb.isEmpty()) throw new JDException("Board not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
            board = bb.get();
        }
        checkManageAccess(board.getProject());
        board.setName(b.getName());
        board.setActive(b.isActive());
        Optional<IssueFilter> filter = issueService.getFilter(b.getFilter().getId());
        if (filter.isPresent() && filter.get().isOpen()) {
            board.setFilter(filter.get());
            if (board.getId() != null && laneRepo.countByBoard(board) <= 0)
                board.setActive(false);
            return boardRepo.save(board);
        }
        throw new JDException("Filter not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public Lane getLane(Long laneID) {
        Optional<Lane> bb = laneRepo.findById(laneID);
        if (bb.isEmpty()) throw new JDException("Lane not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Lane lane = bb.get();
        checkViewAccess(lane.getBoard().getProject());
        return lane;
    }

    public Lane saveLane(Long boardID, Lane l) {
        Lane lane;
        if (l.getId() == null) {
            lane = new Lane();
            lane.setBoard(getBoard(boardID));
        } else {
            Optional<Lane> bb = laneRepo.findById(l.getId());
            if (bb.isEmpty()) throw new JDException("Lane not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
            lane = bb.get();
        }
        checkManageAccess(lane.getBoard().getProject());
        lane.setName(l.getName());
        lane.setLaneOrder(l.getLaneOrder());
        lane.setStatuses(l.getStatuses());
        return laneRepo.save(lane);
    }

    private boolean checkManageAccess(Project project) {
        if (!projectService.hasProjectManageAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return true;
    }

    private boolean checkViewAccess(Project project) {
        if (!projectService.hasProjectViewAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return true;
    }

    private boolean hasManageAccess(Project project) {
        return projectService.hasProjectManageAccess(project);
    }

    private boolean hasViewAccess(Project project) {
        return projectService.hasProjectViewAccess(project);
    }

    public void removeBoard(String projectKey, Board b) {
        Project project = projectService.findByKey(projectKey);
        Board board;
        if (b.getId() == null) {
            board = new Board();
            board.setProject(project);
        } else {
            Optional<Board> bb = boardRepo.findById(b.getId());
            if (!bb.isPresent()) throw new JDException("Board not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
            board = bb.get();
        }
        if (null == board) throw new JDException("Board not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        checkManageAccess(board.getProject());
        laneRepo.deleteAll(laneRepo.findAllByBoardOrderByLaneOrderAsc(board));
        boardRepo.delete(board);
    }

    public void removeLane(Long boardID, Lane l) {
        Lane lane;
        if (l.getId() == null) {
            lane = new Lane();
            lane.setBoard(getBoard(boardID));
        } else {
            Optional<Lane> bb = laneRepo.findById(l.getId());
            if (bb.isEmpty()) throw new JDException("Lane not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
            lane = bb.get();
        }
        if (null == lane) throw new JDException("Lane not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        checkManageAccess(lane.getBoard().getProject());
        laneRepo.delete(lane);
        checkBoard(lane.getBoard());
    }

    private void checkBoard(Board board) {
        if (laneRepo.countByBoard(board) <= 0) {
            board.setActive(false);
            boardRepo.save(board);
        }
    }

}
