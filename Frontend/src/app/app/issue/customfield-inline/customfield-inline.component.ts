import { ProjectService } from './../../project-manage/project.service';
import { IssueService } from './../issue.service';
import { Issue, Version } from './../issue';
import { Component, OnInit, Output, Input, EventEmitter } from '@angular/core';
import { JDToastrService } from '../../toastr.service';
import { JDUser } from '../../admin/user/user';

@Component({
  selector: 'app-customfield-inline',
  templateUrl: './customfield-inline.component.html',
  styleUrls: ['./customfield-inline.component.css']
})
export class CustomfieldInlineComponent implements OnInit {
  @Output()
  saveEvent: any = new EventEmitter();
  edit = false;
  pencil = false;
  orgValue = undefined;
  orgValues = undefined;
  @Input() issue: Issue = undefined;
  @Input() field = undefined;
  @Input() editable = false;
  members: JDUser[];
  versions: Version[];
  constructor(private issueService: IssueService, private toastr: JDToastrService,
    private projectService: ProjectService) { }

  ngOnInit() {
    if (this.field.customField.type === 'SELECT') {
      this.field.customField.values = this.field.customField.value.split(',');
      if (this.field.value && this.field.customField.multiple)
        this.field.values = this.field.value.split(',');
    }
    if (this.field.customField.type === 'USER') {
      this.projectService.getProjectMembers(this.issue.project.key).subscribe(resp => this.members = resp);
    }
    if (this.field.customField.type === 'VERSION') {
      this.projectService.getProjectVersions(this.issue.project.key).subscribe(resp => this.versions = resp);
    }
  }

  saveCustomField(value) {
    if (value == null || value == "null") value = "";
    this.field.value = value;
    if (this.field.customField.multiple) {
      if (this.field.values.length <= 0 && this.field.customField.required) {
        this.toastr.error('Required', this.field.customField.name + ' is required');
        return;
      }
      this.field.value = value.join(",");
    } else {
      if (this.field.value === '' && this.field.customField.required) {
        this.toastr.error('Required', this.field.customField.name + ' is required');
        return;
      }
    }
    this.issueService.saveCustomField(this.issue.project.key, this.issue.key, this.field).subscribe(resp => {
      this.edit = false;
      this.saveEvent.emit({ 'field': this.field, 'result': resp, 'issue': resp.issue });
    });
  }

  enableEdit() {
    if (this.editable) {
      this.orgValue = this.field.value;
      this.orgValues = this.field.values;
      this.edit = true; this.pencil = false;
    }
  }

  cancelEdit() {
    this.edit = false;
    this.field.value = this.orgValue;
    this.field.values = this.orgValues;
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }
}
