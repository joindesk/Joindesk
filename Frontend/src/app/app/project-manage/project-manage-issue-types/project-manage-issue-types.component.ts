import { ProjectService } from './../project.service';
import { Component, OnInit } from '@angular/core';
import { JDToastrService } from '../../toastr.service';
import { ActivatedRoute } from '@angular/router';
import { IssueType } from '../issue-type';
import { Workflow } from '../workflow';
import { IssueTypeService } from '../issue-type.service';
import { WorkflowService } from '../workflow.service';
import { Project } from '../project';


@Component({
  selector: 'app-project-manage-issue-types',
  templateUrl: './project-manage-issue-types.component.html',
  styleUrls: ['./project-manage-issue-types.component.css']
})
export class ProjectManageIssueTypesComponent implements OnInit {
  private projectKey;
  project: Project;
  public issueType: IssueType;
  public applicableWorkflows: Workflow[];
  public applicableWorkflowsToChange: Workflow[];
  public model = {
    changeTo: undefined
  };
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute,
    private workflowService: WorkflowService, public issueTypeService: IssueTypeService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.projectService.getProjectByKey(this.projectKey).subscribe(resp => {
      this.project = resp;
    });
    this.issueTypeService.getIssueTypes(this.projectKey);
    this.issueTypeService.getIssueStatuses();
    this.workflowService.getWorkflows(this.projectKey).subscribe(resp => this.applicableWorkflows = resp);
  }

  openissueType(issueTypeID: number) {
    this.issueTypeService.getIssueType(this.projectKey, issueTypeID).subscribe(resp => {
      this.issueType = resp;
      this.workflowService.getWorkflowsExcept(this.projectKey, this.issueType.workflow.id).
        subscribe(resp2 => this.applicableWorkflowsToChange = resp2);
    }, error => {
      console.log(error);
    });
  }

  save(isActive) {
    this.issueType.active = isActive.checked;
    if (this.issueType.id > 0) {
      this.issueTypeService.save(this.projectKey, this.issueType).subscribe(resp => {
        this.issueType = resp;
        this.issueTypeService.getIssueTypes(this.projectKey);
        this.toastr.success('Saved', 'Issue type');
        this.issueType = undefined;
      }, error => console.error(error));
    } else {
      if (this.issueType.newWorkflow) {
        this.issueType.workflow = new Workflow;
        this.issueType.workflow.id = 0;
      }
      this.issueTypeService.createIssueType(this.projectKey, this.issueType).subscribe(resp => {
        this.issueType = resp;
        this.issueTypeService.getIssueTypes(this.projectKey);
        this.toastr.success('Saved', 'Issue type');
        this.issueType = undefined;
      }, error => console.error(error));
    }
  }

  create() {
    this.issueType = new IssueType();
    this.issueType.edit = true;
  }

  cancel() {
    if (this.issueType.id > 0) {
      this.issueTypeService.closeIssueType();
      this.issueType = undefined;
    } else {
      this.issueType = undefined;
    }
  }

  changeWorkflow(workflowID: number) {
    this.issueTypeService.changeWorkflow(this.projectKey, this.issueType.id, workflowID).
      subscribe(resp => console.log(resp), error => console.log(error));
  }

}
