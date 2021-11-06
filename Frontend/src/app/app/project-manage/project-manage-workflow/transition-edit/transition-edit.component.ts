import { WorkflowTransition } from '../../../project-manage/workflow-transition';
import { JDToastrService } from './../../../toastr.service';
import { WorkflowService } from '../../../project-manage/workflow.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit, Input } from '@angular/core';
import { WorkflowTransitionProperties } from '../../../project-manage/workflow-transition-properties';

@Component({
  selector: 'app-transition-edit',
  templateUrl: './transition-edit.component.html',
  styleUrls: ['./transition-edit.component.css']
})
export class TransitionEditComponent implements OnInit {
  public transitions: any[];
  public transition: WorkflowTransition;
  @Input() public projectKey;
  @Input() public workflowID: number;
  @Input() public transitionID: number;
  public types: [];
  public model = {
    type: undefined,
    condition: "OR",
    subType: undefined,
    subTypeKey: undefined,
    subTypeValue: undefined,
    fields: undefined
  };
  public keyMap = {};
  constructor(private workflowService: WorkflowService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.open();
  }

  open() {
    this.workflowService.getTransitionproperties(this.projectKey).subscribe(resp => {
      this.types = resp;
      resp.forEach(av => {
        Object.keys(av.keyMap).forEach(v => {
          this.keyMap[v] = av.keyMap[v];
        })
      });
    });
    this.getProperties();
  }

  getProperties() {
    this.workflowService.getWorkflowTransitionproperties(this.projectKey, this.workflowID, this.transitionID).subscribe(resp => {
      this.transitions = JSON.parse(resp.transitionProperties);
      this.transition = JSON.parse(resp.transition);
    }, error => {
      console.log(error);
    });
  }

  getTransitions(t: string) {
    return this.transitions.filter((tr: any) => tr.type === t);
  }

  tabChanged($event) {
    this.resetModel();
  }

  resetModel() {
    this.model = {
      type: undefined,
      condition: "OR",
      subType: undefined,
      subTypeKey: undefined,
      subTypeValue: undefined,
      fields: undefined
    };
  }

  changeSubType(t, $event) {
    if ($event.value !== undefined) {
      this.model.type = t.key;
      this.model.condition = "OR";
      this.workflowService.getWorkflowTransitionPropertyFields(this.projectKey, this.workflowID, this.transitionID, this.model.subType)
        .subscribe(resp => {
          this.model.fields = resp;
        });
    }
  }

  addCondition() {
    const t = new WorkflowTransitionProperties;
    t.type = this.model.type;
    t.condition = this.model.condition;
    t.subType = this.model.subType;
    if (this.model.subTypeKey != undefined) {
      if (this.model.subTypeKey.label) {
        t.key = this.model.subTypeKey.label;
      } else {
        if (this.model.fields.type === 'select-multiple') {
          t.key = this.model.subTypeKey.toString();
        } else {
          t.key = this.model.subTypeKey;
        }
      }
      t.value = this.model.subTypeValue;
      this.workflowService.saveTransitionProperties(this.projectKey, this.workflowID, this.transitionID, t).subscribe(resp => {
        this.toastr.success(t.type, ' added');
        this.resetModel();
        this.getProperties();
      });
    } else {
      this.toastr.warn("Validation Failed", "No option selected");
    }
  }

  removeTransitionProperty(tr) {
    this.toastr.confirm("").then(result => {
      if (result.value) {
        this.workflowService.deleteTransitionProperties(this.projectKey, this.workflowID, this.transitionID, tr).subscribe(resp => {
          this.toastr.success(tr.type, 'removed');
          this.resetModel();
          this.getProperties();
        });
      }
    });
  }

}
