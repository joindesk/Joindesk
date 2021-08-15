import { Component, OnInit, Input } from '@angular/core';
import { NzModalRef } from 'ng-zorro-antd';
import { IssueService } from '../issue.service';
import { JDToastrService } from '../../toastr.service';
import { WorkLog } from '../issue';

@Component({
  selector: 'app-worklog-dialog',
  templateUrl: './worklog-dialog.component.html',
  styleUrls: ['./worklog-dialog.component.css']
})
export class WorklogNzDialogComponent implements OnInit {
  @Input() workLog: WorkLog;
  loading = false;
  constructor(private modal: NzModalRef, private issueService: IssueService,
    private toast: JDToastrService) { }

  ngOnInit() {
  }

  save() {
    this.loading = true;
    if (!this.workLog.issue) {
      const projectKey = this.workLog.issueKey.substr(0, this.workLog.issueKey.indexOf('-'));
      const issueKey = +this.workLog.issueKey.substr(this.workLog.issueKey.indexOf('-') + 1, this.workLog.issueKey.length);
      this.issueService.saveWorkLog(projectKey, issueKey, this.workLog).subscribe(resp => {
        this.toast.success('Work Log', ' Logged');
        this.loading = false;
        this.destroyModal();
      });
    } else {
      this.workLog.issue.workLogs = [];
      this.issueService.saveWorkLog(this.workLog.issue.project.key, +this.workLog.issue.key, this.workLog).subscribe(resp => {
        this.toast.success('Work Log', ' Logged');
        this.loading = false;
        this.destroyModal();
      });
    }
  }

  delete() {
    if (this.workLog.id > 0) {
      this.toast.confirm("Sure to delete ?").then(val => {
        if (val.value) {
          this.issueService.removeWorkLog(this.workLog.issue.project.key, +this.workLog.issue.key, this.workLog.id).subscribe(resp => {
            this.toast.success('Work Log', 'Deleted');
            this.destroyModal();
          });
        }
      });
    }
  }

  destroyModal(): void {
    this.modal.destroy();
  }

}
