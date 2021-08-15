import { IssueService } from './../issue/issue.service';
import { JDToastrService } from './../toastr.service';
import { MatDialog, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Component, OnInit, Inject } from '@angular/core';
import { Issue, WorkLog } from '../issue/issue';

@Component({
  selector: 'app-worklog-dialog',
  templateUrl: './worklog-dialog.component.html',
  styleUrls: ['./worklog-dialog.component.css']
})
export class WorklogDialogComponent implements OnInit {
  issue: Issue;
  workLog: WorkLog = new WorkLog;
  constructor(private issueService: IssueService, private toast: JDToastrService,
    @Inject(MAT_DIALOG_DATA) public data: { issue: Issue, worklog: WorkLog }, private dialog: MatDialog) {
    this.issue = data.issue;
    this.workLog = data.worklog;
    if (this.workLog.workFrom == undefined) {
      this.workLog.workFrom = new Date();
    }
    this.workLog = Object.assign({}, this.workLog);
    this.workLog.issue = this.issue;
  }

  ngOnInit() { }

  save() {
    this.workLog.issue = undefined;
    this.issueService.saveWorkLog(this.issue.project.key, +this.issue.key, this.workLog).subscribe(resp => {
      this.toast.success('Work Logged', ' Work Logged');
      this.dialog.closeAll();
    });
  }

  close() {
    this.dialog.closeAll();
  }

}
