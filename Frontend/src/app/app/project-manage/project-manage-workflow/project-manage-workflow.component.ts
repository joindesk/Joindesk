import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { WorkflowService } from '../workflow.service';
import { IssueTypeService } from '../issue-type.service';
import { Workflow } from '../workflow';

@Component({
  selector: 'app-project-manage-workflow',
  templateUrl: './project-manage-workflow.component.html',
  styleUrls: ['./project-manage-workflow.component.css']
})
export class ProjectManageWorkflowComponent implements OnInit {
  projectKey;
  public workflows: Workflow[];
  constructor(private route: ActivatedRoute, private issueTypeService: IssueTypeService,
    private workflowService: WorkflowService) {
  }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.fetchWorkflows();
    this.workflowService.changeEvent.subscribe(event => {
      this.fetchWorkflows();
    });
    this.issueTypeService.getIssueStatuses();
  }

  fetchWorkflows() {
    this.workflowService.getWorkflows(this.projectKey).subscribe(resp => this.workflows = resp);
  }

}
