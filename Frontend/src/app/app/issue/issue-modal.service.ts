import { NzModalService } from 'ng-zorro-antd';
import { IssueViewComponent } from './issue-view/issue-view.component';
import { Injectable } from '@angular/core';
import { Issue, WorkLog } from './issue';
import { WorklogDialogComponent } from '../worklog-dialog/worklog-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { WorklogNzDialogComponent } from './worklog-dialog/worklog-dialog.component';
import { Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';

@Injectable()
export class IssueModalService {
  worklogChanged = new Subject();
  constructor(private modal: NzModalService, public dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router, private location: Location) { }

  openIssueModal(keyPair) {
    var currentPath = this.router.url;
    this.location.replaceState('issue/' + keyPair);
    let modal = this.modal.create({
      nzTitle: keyPair,
      nzZIndex: 800,
      nzClassName: 'issueModal',
      nzWidth: window.innerWidth > 900 ? window.innerWidth - 200 : window.innerWidth - 50,
      nzContent: IssueViewComponent,
      nzGetContainer: () => document.body,
      nzComponentParams: {
        issueKeyFromModal: keyPair,
      },
      nzOnOk: () => new Promise(resolve => setTimeout(resolve, 1000)),
    });
    modal.afterClose.subscribe(() => {
      this.location.replaceState(currentPath);
      modal.destroy();
    })
  }

  openIssueWorkLogModal(worklog: WorkLog) {
    const modal = this.modal.create({
      nzTitle: 'Worklog',
      nzZIndex: 800,
      nzContent: WorklogNzDialogComponent,
      nzComponentParams: {
        workLog: worklog
      },
      nzOnOk: () => new Promise(resolve => setTimeout(resolve, 1000)),
    });

    modal.afterClose.subscribe(result => this.worklogChanged.next());
  }

  openIssueWorkModal(issue: Issue, workLog: WorkLog) {
    this.dialog.open(WorklogDialogComponent, {
      width: (window.innerWidth / 3) + 'px',
      data: {
        issue: issue,
        worklog: workLog
      }
    });
  }

  closeAll() {
    this.modal.closeAll();
    this.dialog.closeAll();
  }

}
