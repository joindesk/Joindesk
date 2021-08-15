import { Board, Lane } from './board';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpService } from '../../http.service';

@Injectable()
export class BoardService {
  board: Board;
  constructor(private http: HttpClient) { }

  getBoards(projectKey: string) {
    return this.http.get<Board[]>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/');
  }

  getBoard(projectKey: string, id: number) {
    return this.http.get<Board>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/' + id + '/');
  }

  saveBoard(projectKey: string, board: Board) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/',
      board);
  }

  removeBoard(projectKey: string, board: Board) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/remove',
      board);
  }

  getBoardLane(projectKey: string, boardID: number, laneID: number) {
    return this.http.get<Board>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/' + boardID + '/lane/' + laneID + '/');
  }

  saveBoardLane(projectKey: string, lane: Lane) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/' + lane.board.id + '/lane/',
      lane);
  }

  removeBoardLane(projectKey: string, lane: Lane) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/board/' + lane.board.id + '/lane/remove',
      lane);
  }
}
