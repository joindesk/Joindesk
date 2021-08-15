import { DashboardService } from './../../dashboard.service';
import { BoardService } from './../board.service';
import { IssueService } from './../../../issue/issue.service';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { Component, OnInit, TemplateRef, AfterViewInit } from '@angular/core';
import { Board } from '../board';
import { IssueFilter } from '../../../issue/issue';
import { JDToastrService } from '../../../toastr.service';
import { NzModalService } from 'ng-zorro-antd';

@Component({
  selector: 'app-board-overview',
  templateUrl: './board-overview.component.html',
  styleUrls: ['./board-overview.component.css']
})
export class BoardOverviewComponent implements OnInit {
  boards: Board[];
  filters: IssueFilter[];
  board: Board;
  boardForEdit: Board;
  projectKey: string;
  boardID: number;
  editable: boolean;
  constructor(
    private dashboardService: DashboardService, public route: ActivatedRoute, private toastr: JDToastrService,
    private issueService: IssueService, private boardService: BoardService, private router: Router, private modal: NzModalService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    this.getBoards();
    setTimeout(() => {
      if (this.dashboardService.currentProject) { this.editable = this.dashboardService.currentProject.editable; }
      this.issueService.getOpenFilters(this.projectKey).subscribe(resp => {
        this.filters = resp;
      });
    }, 500);
  }

  getCurrentBoard() {
    this.board = this.boardService.board;
    return this.boardService.board;
  }

  back() {
    this.board = undefined;
    this.boardService.board = undefined;
    this.router.navigateByUrl('/project/' + this.projectKey + '/board');
  }

  changeBoard() {
    this.router.navigate(['project', this.projectKey, 'board', this.board.id], { queryParams: { 'd': Date.now() } });
  }

  getBoards() {
    if (this.route.snapshot.children.length > 0) {
      this.boardID = +this.route.snapshot.children[0].paramMap.get('boardID');
    }
    this.boardService.getBoards(this.projectKey).subscribe(resp => {
      this.boards = resp;
      if (this.boardID > 0) {
        this.boards.forEach(board => {
          if (this.boardID === board.id) {
            this.board = board;
          }
        });
      } //else if (this.boards.length > 0) {
      //   this.board = this.boards[0];
      //   // If any active board, load first
      //   this.boards.forEach(b => {
      //     if (b.active) {
      //       this.board = b;
      //     }
      //   });
      //   this.changeBoard();
      // }
    });
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }

  editBoard(tplContent: TemplateRef<{}>): void {
    this.boardForEdit = JSON.parse(JSON.stringify(this.board));
    this.boardForEdit.edit = true;
    this.openModal(tplContent);
  }

  create(tplContent: TemplateRef<{}>): void {
    this.boardForEdit = new Board();
    this.boardForEdit.active = true;
    this.boardForEdit.edit = true;
    this.openModal(tplContent);
  }

  openModal(tplContent: TemplateRef<{}>) {
    this.modal.create({
      nzContent: tplContent,
      nzClosable: false,
      nzComponentParams: {
        data: this.boardForEdit
      },
      nzOnOk: () => {
        if (!this.boardForEdit.name || this.boardForEdit.name.length <= 0) {
          this.toastr.error('Name is required', 'validation failed');
          return false;
        } else if (!this.boardForEdit.filter) {
          this.toastr.error('Filter is required', 'validation failed');
          return false;
        } else
          this.save(this.boardForEdit);
      },
      nzOnCancel: () => {
        this.boardForEdit = undefined;
        this.getCurrentBoard();
      }
    });
  }

  delete(board: Board) {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.boardService.removeBoard(this.projectKey, board).subscribe(resp => {
          this.board = undefined;
          this.boardForEdit = undefined;
          this.toastr.success('Deleted', 'Board deleted');
          this.getBoards();
        });
      }
    });
  }

  save(board: Board) {
    this.boardService.saveBoard(this.projectKey, board).subscribe(resp => {
      this.boardService.board = resp;
      this.boardForEdit = undefined;
      this.toastr.success('Saved', 'Board');
      this.router.navigate(['project', this.projectKey, 'board', resp.id], { queryParams: { 'd': Date.now() } });
      this.getCurrentBoard();
      this.getBoards();
    });
  }

}
