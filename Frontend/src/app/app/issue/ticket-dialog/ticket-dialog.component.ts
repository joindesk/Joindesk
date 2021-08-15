import { IssueService } from './../issue.service';
import { Router, ActivatedRoute } from '@angular/router';
import { ProjectService } from './../../project-manage/project.service';
import { Project } from './../../project-manage/project';
import { IssueTypeService } from '../../project-manage/issue-type.service';
import { AuthenticationService } from './../../../auth/authentication.service';
import { Component, OnInit, Inject, Input, ViewChild, TemplateRef } from '@angular/core';
import { Issue } from '../issue';
import { IssueType } from '../../project-manage/issue-type';
import { JDUser } from '../../admin/user/user';
import { JDToastrService } from '../../toastr.service';
import { NzDrawerRef } from 'ng-zorro-antd';
import { MyaccountService } from '../../myaccount/myaccountservice.service';
@Component({
  selector: 'app-ticket-dialog',
  templateUrl: './ticket-dialog.component.html',
  styleUrls: ['./ticket-dialog.component.css']
})
export class TicketDialogComponent implements OnInit {
  @Input() projectKey = '';
  issue: Issue;
  isCreating = false;
  currentProject: Project;
  createableProjects: Project[] = [];
  issueTypes: IssueType[];
  assignableMembers: JDUser[];
  showAssignToSelf = false;
  constructor(public authenticationService: AuthenticationService, private issueTypeService: IssueTypeService,
    private issueService: IssueService, private projectService: ProjectService, private toast: JDToastrService,
    private router: Router, private route: ActivatedRoute, private drawerRef: NzDrawerRef<any>,
    private myaccountService: MyaccountService) {
    //const projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    this.issue = new Issue();
    this.issue.permissions = {
      'edit': true, 'comment': false, 'delete': false, 'assign': false, 'reporter': false, 'link': false, 'attach': false
    };
    if (this.authenticationService.projects) {
      authenticationService.projects.forEach(project => {
        if (this.drawerRef['nzContentParams'].value === project.key) {
          this.currentProject = project;
          this.issue.project = project;
        }
      });
    } else {
      this.router.navigate(['project', this.projectKey, { r: 'create' }]);
    }
    this.issue.priority = 'Normal';
    this.changeProject();
  }

  close(): void {
    this.drawerRef.close('test');
  }

  ngOnInit() {
    this.authenticationService.projects.forEach(p => {
      if (p.authorities.includes('ISSUE_CREATE')) {
        this.createableProjects.push(p);
      }
    });
    this.getProjectMembers();
    this.route.params.subscribe(params => {
      const c = params['c'];
      if (c && c !== '') {
        this.get(c);
      }
    });
  }

  compareByID(itemOne, itemTwo) {
    return itemOne && itemTwo && itemOne.id === itemTwo.id;
  }

  getProjectMembers() {
    this.projectService.getProjectMembers(this.issue.project.key).subscribe(resp => {
      this.assignableMembers = resp;
      window['members'] = resp;
      this.assignableMembers.forEach(m => {
        if (m.id === this.myaccountService.user.id) {
          this.issue.reporter = m;
          this.showAssignToSelf = true;
        }
      });
    });
  }

  assignToMe() {
    this.assignableMembers.forEach(m => {
      if (m.id === this.myaccountService.user.id) {
        this.issue.assignee = m;
      }
      this.showAssignToSelf = false;
    });
  }

  get(issueKey: number) {
    this.issueService.getIssue(this.currentProject.key, issueKey).subscribe(resp => {
      this.issue.summary = resp.summary;
      this.issue.description = resp.description;
      this.issue.assignee = resp.assignee;
      this.issue.reporter = resp.reporter;
      this.issue.priority = resp.priority;
      this.issue.issueType = resp.issueType;
    });
  }

  changeProject() {
    this.getProjectMembers();
    this.issueTypeService.getIssueTypesObservable(this.issue.project.key).subscribe(resp => {
      this.issueTypes = resp;
      if (this.issueTypes.length > 0) {
        this.issue.issueType = resp[0];
      }
    });
  }

  save() {
    this.isCreating = true;
    this.issueService.createIssue(this.issue.project.id, this.issue).subscribe(
      resp => {
        this.issue = resp;
        this.isCreating = false;
        this.issueService.issueCreateNotification.next(this.issue);
        //this.router.navigate(['/issue', this.issue.project.key + '-' + this.issue.key]);
        this.close();
      }
    );
  }

  compareFn(a: any, b: any) {
    return a && b ? a.id === b.id : a === b;
  }

}
