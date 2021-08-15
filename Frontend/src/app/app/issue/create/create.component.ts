import { Component, OnInit, Input } from '@angular/core';
import { Issue } from '../issue';
import { Project } from '../../project-manage/project';
import { IssueType } from '../../project-manage/issue-type';
import { JDUser } from '../../admin/user/user';
import { AuthenticationService } from '../../../auth/authentication.service';
import { IssueTypeService } from '../../project-manage/issue-type.service';
import { JDToastrService } from '../../toastr.service';
import { ProjectService } from '../../project-manage/project.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MyaccountService } from '../../myaccount/myaccountservice.service';
import { IssueService } from '../issue.service';
import { DashboardService } from '../../dashboard/dashboard.service';
@Component({
  selector: 'app-create',
  templateUrl: './create.component.html',
  styleUrls: ['./create.component.css']
})
export class CreateComponent implements OnInit {
  visible = true;
  @Input() projectKey = '';
  issue: Issue;
  isCreating = false;
  isMinimized = false;
  openAfterCreate = false;
  closeAfterCreate = true;
  currentProject: Project;
  createableProjects: Project[] = [];
  issueTypes: IssueType[];
  assignableMembers: JDUser[];
  showAssignToSelf = false;
  constructor(public authenticationService: AuthenticationService, private issueTypeService: IssueTypeService,
    private issueService: IssueService, private projectService: ProjectService, private router: Router,
    private route: ActivatedRoute, private dashboardService: DashboardService,
    private myaccountService: MyaccountService, private toastr: JDToastrService) {
    //const projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    this.issue = new Issue();
    this.issue.description = '';
    this.issue.permissions = {
      'edit': true, 'comment': false, 'delete': false, 'assign': false, 'reporter': false, 'link': false, 'attach': false
    };
    if (this.authenticationService.projects) {
      authenticationService.projects.forEach(project => {
        if (this.dashboardService.currentProject.key === project.key) {
          this.currentProject = project;
          this.issue.project = project;
        }
      });
    } else {
      alert("Error getting projects");
    }
    this.issue.priority = 'Normal';
    this.changeProject();
  }

  ngOnInit() {
    this.authenticationService.projects.forEach(p => {
      if (p.authorities.includes('ISSUE_CREATE')) {
        this.createableProjects.push(p);
      }
    });
    if (this.createableProjects.length == 0) {
      this.close();
      this.toastr.errorMessage("No permissions to create issue");
    } else {
      this.getProjectMembers();
      this.route.params.subscribe(params => {
        const c = params['c'];
        if (c && c !== '') {
          this.get(c);
        }
      });
    }
  }

  calcWidth() {
    return this.isMinimized ? 20 : (window.innerWidth < 600) ? 90 : (window.innerWidth <= 1080) ? 60 : (window.innerWidth <= 1440) ? 50 : 40;
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
        this.issue = resp.issue;
        this.isCreating = false;
        this.issueService.issueCreateNotification.next(this.issue);
        //this.router.navigate(['/issue', this.issue.project.key + '-' + this.issue.key]);
        if (this.openAfterCreate) this.router.navigate(['/issue', this.issue.keyPair]);
        if (this.closeAfterCreate)
          this.close();
        else
          this.issue = new Issue();
      }
    );
  }

  compareFn(a: any, b: any) {
    return a && b ? a.id === b.id : a === b;
  }

  close() {
    this.issue = new Issue();
    this.visible = false;
  }

}
