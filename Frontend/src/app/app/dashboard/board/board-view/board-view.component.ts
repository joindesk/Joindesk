import { JDToastrService } from './../../../toastr.service';
import { DashboardService } from './../../dashboard.service';
import { BoardService } from './../board.service';
import { IssueService } from './../../../issue/issue.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit, Pipe, PipeTransform, TemplateRef, ViewChild } from '@angular/core';
import { Board, Lane, BoardFilter } from '../board';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { JDUser } from '../../../admin/user/user'
import { Issue } from '../../../issue/issue'
import { IssueModalService } from '../../../issue/issue-modal.service';

@Component({
  selector: 'app-board-view',
  templateUrl: './board-view.component.html',
  styleUrls: ['./board-view.component.css']
})
export class BoardViewComponent implements OnInit {
  projectKey: string;
  boardID: number;
  board: Board;
  editable: boolean;
  filterArgs = new BoardFilter();
  members = []; labels = [];
  loading = true;
  lane: Lane;
  showLaneEdit = false;
  issueKeys = [];
  colors = ['rgb(85, 189, 40)', 'rgb(153, 0, 0)	', 'rgb(204, 0, 0)', 'rgb(17, 85, 204)', 'rgb(241, 194, 50)', 'rgb(230, 145, 56)', 'rgb(103, 78, 167)', 'rgb(0, 175, 224)'];
  constructor(private issueService: IssueService, private issueModalService: IssueModalService,
    public route: ActivatedRoute, private boardService: BoardService, private router: Router,
    private dashboardService: DashboardService, private toast: JDToastrService) { }

  ngOnInit() {
    this.filterArgs.member = [];
    this.filterArgs.labels = [];
    setTimeout(() => {
      this.boardID = +this.route.snapshot.paramMap.get('boardID');
      this.route.paramMap.subscribe(p => {
        this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
        if (p.get('boardID') !== undefined) {
          this.boardID = +p.get('boardID');
        }
        this.get();
      });
      if (this.dashboardService.currentProject) { this.editable = this.dashboardService.currentProject.editable; }
      this.route.queryParams.subscribe(queryParams => {
        this.get();
      });
    }, 500);
    this.issueService.issueNotification.subscribe(resp => {
      resp = JSON.parse(resp);
      if (this.dashboardService.currentProjectKey && resp["project"] == this.dashboardService.currentProjectKey)
        this.issueKeys.filter(i => i.key == resp["issue"]).forEach(i => {
          if (i.updated != resp["updated"]) {
            this.get();
          }
        })
    })
  }

  getRandomColor(s) {
    return this.colors[this.hashCode(s) % 8];
  }

  hashCode(s) {
    return s.split("").reduce(function (a, b) { a = ((a << 5) - a) + b.charCodeAt(0); return a & a }, 0);
  }

  get() {
    this.loading = true;
    this.boardService.getBoard(this.projectKey, this.boardID).subscribe(resp => {
      this.board = resp;
      this.boardService.board = resp;
      this.getFilters();
      this.dashboardService.setBoardForUser(this.projectKey, resp.id);
    }, error => {
      this.router.navigateByUrl('project/' + this.projectKey + '/board');
    });
  }

  getFilters() {
    this.issueKeys = [];
    this.board.lanes.forEach(l => {
      l['color'] = this.getRandomColor(l.name);
      l.issues.forEach(i => {
        this.issueKeys.push({ 'key': i.keyPair, 'updated': i.updated });
        if (i.assignee && !this.members.find(m => m.id === i.assignee.id)) {
          this.members.push(i.assignee);
        }
        i.labels.forEach(l => {
          if (!this.labels.find(m => m.id === l.id)) {
            this.labels.push(l);
          }
        });
      });
    });
    this.loading = false;
  }

  getAndEdit(lane) {
    this.boardService.getBoard(this.projectKey, this.boardID).subscribe(resp => {
      this.board = resp;
      this.board.lanes.forEach(l => {
        if (l.name === lane.name) {
          this.editLane(l);
        }
      });
      this.getFilters();
    });
  }

  createLane() {
    const l = new Lane();
    l.board = this.board;
    l.laneOrder = 1;
    this.editLane(l);
  }

  addLane() {
    const lane = new Lane();
    lane.statuses = [];
    let maxOrder = 1;
    this.board.lanes.forEach(l => {
      if (l.laneOrder >= maxOrder) {
        maxOrder = l.laneOrder + 1;
      }
    });
    lane.laneOrder = maxOrder;
    lane.board = this.board;
    lane.name = 'Lane ' + maxOrder;
    this.boardService.saveBoardLane(this.projectKey, lane).subscribe(resp => {
      this.toast.success('Lane created', lane.name);
      this.get();
      this.getAndEdit(lane);
    });
  }

  editLane(lane: Lane) {
    lane.board = this.board;
    lane.board.lanes = [];
    console.log(lane);
    this.lane = lane;
    this.showLaneEdit = true;
  }

  saveLane() {
    if (!this.lane.id || this.lane.id <= 0) {
      this.lane.statuses = [];
    }
    this.boardService.saveBoardLane(this.projectKey, this.lane).subscribe(resp => {
      this.toast.success('Lane updated', this.lane.name);
      this.get();
      this.showLaneEdit = false;
    });
  }

  deleteLane(lane) {
    this.boardService.removeBoardLane(this.projectKey, lane).subscribe(resp => {
      this.toast.success('Lane removed', lane.name);
      this.get();
      this.showLaneEdit = false;
    });
  }

  toggleMember(m: JDUser) {
    const indx = this.filterArgs.member.findIndex(mem => mem.id === m.id);
    if (indx > -1) {
      this.filterArgs.member.splice(indx, 1);
    } else {
      this.filterArgs.member.push(m);
    }
  }

  highLightMember(m: JDUser) {
    if (this.filterArgs.member.length <= 0) return 1;
    if (this.filterArgs.member.length > 0 && this.filterArgs.member.findIndex(mem => mem.id === m.id) > -1) return 1;
    return 0;
  }

  drop(event: CdkDragDrop<string[]>) {
    if (event.previousContainer !== event.container) {
      console.log('Move it to ' + event.container.id);
      this.issueService.laneTransition(this.projectKey,
        +event.item.element.nativeElement.dataset['issue'],
        +event.container.element.nativeElement.dataset['lane']).subscribe(resp =>
          this.get()
        );
    }
  }

  searchByAssignee(label: string) {
    this.navigate('assignee:' + label);
  }

  searchByPriority(label: string) {
    this.navigate('priority:' + label);
  }

  searchByStatus(label: string) {
    this.navigate('current_step:' + label);
  }

  navigate(q) {
    this.router.navigateByUrl('project/' + this.projectKey + '?q=' + q);
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }

  openIssue(keyPair) {
    //this.router.navigateByUrl('/issue/' + $event.event.id);
    this.issueModalService.openIssueModal(keyPair);
  }
}


@Pipe({ name: 'boardFilter', pure: false })
export class BoardFilterPipe implements PipeTransform {
  transform(items: Issue[], filter: BoardFilter): any {
    if (!items || !filter || (filter.member.length < 0 && filter.labels.length < 0)) {
      return items;
    }
    const filtered = [];
    if (filter.member.length > 0) {
      items.filter(item => {
        if (item.assignee && filter.member.find(m => m.id === item.assignee.id)) return 1;
      }).forEach(i => filtered.push(i));
    } else {
      items.forEach(i => filtered.push(i));
    }
    // if (filter.labels.length > 0) {
    //   filtered.filter(item => {
    //     if (item.labels.length > 0 && filter.labels.find(m => m.id === item.assignee.id)) return 1;
    //   }).forEach(i => filtered.push(i));
    // }
    return filtered;
  }
}