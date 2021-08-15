import { IssueTypeService } from '../../../project-manage/issue-type.service';
import { WorkflowStep, IssueStatus } from '../../../project-manage/workflow-step';
import { WorkflowTransition } from '../../../project-manage/workflow-transition';
import { JDToastrService } from './../../../toastr.service';
import { WorkflowService } from '../../../project-manage/workflow.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Workflow } from '../../../project-manage/workflow';
import { Component, OnInit } from '@angular/core';
import { NzModalService } from 'ng-zorro-antd';
import { TransitionEditComponent } from '../transition-edit/transition-edit.component';

@Component({
  selector: 'app-workflow-edit',
  templateUrl: './workflow-edit.component.html',
  styleUrls: ['./workflow-edit.component.css']
})
export class WorkflowEditComponent implements OnInit {
  public workflow: Workflow;
  public projectKey;
  public workflowID: string;
  public issueStatus: IssueStatus;
  public model = {
    newStep: undefined,
    status: undefined
  };
  public showAddStep = false; public showAddTransition = false;
  public transition: WorkflowTransition = new WorkflowTransition();
  public step: WorkflowStep = new WorkflowStep();
  constructor(private route: ActivatedRoute, private router: Router, private modalService: NzModalService,
    private workflowService: WorkflowService, private toastr: JDToastrService, private issueTypeService: IssueTypeService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.parent.paramMap.get('projectKey');
    this.workflowID = this.route.snapshot.paramMap.get('workflowID');
    this.open();
    this.route.params.subscribe(params => {
      this.workflowID = params['workflowID'];
      this.open();
    });
  }

  open() {
    this.showAddStep = false;
    this.showAddTransition = false;
    if (this.workflowID === 'create') {
      this.workflow = new Workflow();
    } else {
      this.openworkflow(+this.workflowID);
    }
  }

  openworkflow(workflowID: number) {
    this.showAddStep = false;
    this.showAddTransition = false;
    this.workflowService.getWorkflow(this.projectKey, workflowID).subscribe(resp => {
      this.workflow = resp;
    }, error => {
      console.log(error);
    });
  }

  save() {
    this.showAddStep = false;
    this.showAddTransition = false;
    if (this.workflow.id > 0) {
      this.workflowService.save(this.projectKey, this.workflow).subscribe(resp => {
        this.workflow = resp;
        this.workflowService.changeEvent.next();
        this.toastr.success('Workflow', 'Updated');
      }, error => console.error(error));
    } else {
      this.workflowService.createWorkflow(this.projectKey, this.workflow).subscribe(resp => {
        this.workflow = resp;
        this.workflowService.changeEvent.next();
        this.toastr.success('Workflow', 'Created');
        this.router.navigate(['project', this.projectKey, 'manage', 'workflow', this.workflow.id]);
      }, error => console.error(error));
    }
  }

  changeStatus($event) {
    if ($event === 'new') {
      this.issueStatus = new IssueStatus();
    } else {
      this.issueStatus = $event;
    }
  }

  saveStep() {
    debugger
    if (this.step.id > 0) {
      this.step.workflow = this.workflow;
      this.step.issueStatus = this.issueStatus;
      this.step.issueStatus = this.model.status;
      this.workflowService.updateWorkflowStep(this.projectKey, this.workflow.id, this.step).subscribe(resp => {
        this.toastr.success('step', 'step updated');
        this.openworkflow(this.workflow.id);
        this.step = new WorkflowStep();
      });
    } else {
      this.step.issueStatus = this.issueStatus;
      this.workflowService.addWorkflowStep(this.projectKey, this.workflow.id, this.step).subscribe(resp => {
        this.toastr.success('step', 'step added');
        this.openworkflow(this.workflow.id);
        this.step = new WorkflowStep();
      });
    }
  }

  saveTransition() {
    if (this.transition.fromAll)
      this.transition.fromStep = undefined;
    if (this.transition.id > 0) {
      this.transition.workflow = this.workflow;
      this.workflowService.updateWorkflowTransition(this.projectKey, this.workflow.id, this.transition).subscribe(resp => {
        this.toastr.success('Transition', 'Transition updated');
        this.openworkflow(this.workflow.id);
        this.transition = new WorkflowTransition();
      });
    } else {
      this.workflowService.addWorkflowTransition(this.projectKey, this.workflow.id, this.transition).subscribe(resp => {
        this.toastr.success('Transition', 'Transition added');
        this.transition = new WorkflowTransition();
        this.openworkflow(this.workflow.id);
      });
    }
  }

  removeStep(StepID: number) {
    this.toastr.confirm("").then(result => {
      if (result.value) {
        this.workflowService.removeWorkflowStep(this.projectKey, this.workflow.id, StepID).subscribe(resp => {
          this.toastr.success('Step', 'Step Removed');
          this.openworkflow(+this.workflowID);
        });
      }
    });
  }

  removeTransition(transitionID: number) {
    this.toastr.confirm("").then(result => {
      if (result.value) {
        this.workflowService.removeWorkflowTransition(this.projectKey, this.workflow.id, transitionID).subscribe(resp => {
          this.toastr.success('Transition', 'Transition Removed');
          this.openworkflow(+this.workflowID);
        });
      }
    });
  }

  openTransition(transitionID: number) {
    this.modalService.create({
      nzTitle: 'Transition',
      nzClosable: true,
      nzMaskClosable: false,
      nzWidth: '60vw',
      nzContent: TransitionEditComponent,
      nzComponentParams: {
        workflowID: this.workflow.id,
        projectKey: this.projectKey,
        transitionID: transitionID
      },
    });
  }

  create() {
    this.workflow = new Workflow();
  }

  editTransition(wt: WorkflowTransition) {
    this.toastr.success('s', 's');
  }

  compareFn(user1: any, user2: any) {
    return user1 && user2 ? user1.id === user2.id : user1 === user2;
  }

}
