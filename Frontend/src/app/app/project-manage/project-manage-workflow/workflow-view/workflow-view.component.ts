import { JDToastrService } from './../../../toastr.service';
import { WorkflowService } from '../../../project-manage/workflow.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Workflow } from '../../../project-manage/workflow';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-workflow-view',
  templateUrl: './workflow-view.component.html',
  styleUrls: ['./workflow-view.component.css']
})
export class WorkflowViewComponent implements OnInit {
  public workflow: Workflow;
  public projectKey: string;
  public workflowID: number;
  constructor(private route: ActivatedRoute, private router: Router,
    private workflowService: WorkflowService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.parent.paramMap.get('projectKey');
    this.workflowID = +this.route.snapshot.paramMap.get('workflowID');
    this.openworkflow(this.workflowID);
    this.route.params.subscribe(params => {
      this.workflowID = +params['workflowID'];
      this.openworkflow(this.workflowID);
    });
  }

  openworkflow(workflowID: number) {
    this.workflowService.getWorkflow(this.projectKey, workflowID).subscribe(resp => {
      this.workflow = resp;
    }, error => {
      console.log(error);
    });
  }
}
