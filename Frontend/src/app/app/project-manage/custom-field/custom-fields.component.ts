import { IssueTypeService } from './../issue-type.service';
import { ActivatedRoute } from '@angular/router';
import { JDToastrService } from './../../toastr.service';
import { CustomFieldService } from './custom-field.service';
import { Component, OnInit, ViewChild } from '@angular/core';
import { CustomField } from './custom-field';
import { IssueType } from '../issue-type';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';

@Component({
  selector: 'app-custom-fields',
  templateUrl: './custom-fields.component.html',
  styleUrls: ['./custom-fields.component.css']
})
export class CustomFieldsComponent implements OnInit {
  private projectKey;
  public customFields: CustomField[];
  public customField: CustomField;
  public issueTypes: IssueType[];
  public multipleValues: string[];
  displayedColumns: string[] = ['key', 'name', 'type', 'action'];
  dataSource;
  @ViewChild(MatSort, { static: true }) sort: MatSort;
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  constructor(private customFieldService: CustomFieldService, private toastr: JDToastrService, private route: ActivatedRoute,
    private issueTypeService: IssueTypeService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.issueTypeService.getIssueTypesObservable(this.projectKey).subscribe(resp => {
      this.issueTypes = resp;
    });
    this.getFields();
  }

  getFields() {
    this.customFieldService.getAll(this.projectKey).subscribe(resp => {
      this.customFields = (resp || []).sort((a: any, b: any) => a.id < b.id ? -1 : 1);
    });
  }

  open(id: number) {
    this.customFieldService.get(this.projectKey, id).subscribe(resp => {
      this.customField = resp;
      if (this.customField.multiple && this.customField.value)
        this.multipleValues = this.customField.value.split(",");
    });
  }

  save() {
    if (this.isMultipleType() && (this.customField.type != 'USER' && this.customField.type != 'VERSION'))
      this.customField.value = this.multipleValues.join(",");
    if (this.customField.type == "SELECT" && this.customField.defaultValue
      && this.multipleValues && !this.multipleValues.includes(this.customField.defaultValue, 0)) {
      this.toastr.errorMessage("Default Value does not match with Values list")
      return;
    }
    this.customFieldService.save(this.projectKey, this.customField).subscribe(resp => {
      this.customField = resp;
      this.getFields();
      this.toastr.success('Custom Field', 'saved');
      this.multipleValues = [];
    }, error => console.error(error));
  }

  create() {
    this.customField = new CustomField();
    this.customField.edit = true;
    this.customField.type = 'TEXT';
  }

  cancel() {
    if (this.customField.id > 0) {
      this.open(this.customField.id);
    } else {
      this.customField = undefined;
    }
  }

  compareFn(a: any, b: any) {
    return a && b ? a.id === b.id : a === b;
  }

  isMultipleType() {
    const typ = this.customField.type;
    return typ === 'SELECT' || typ === 'USER' || typ === 'VERSION';
  }

  typeChange($event) {
    // listen to type changes
  }

  delete() {
    this.toastr.confirm('All field data will be lost').then((result) => {
      if (result.value) {
        this.customFieldService.delete(this.projectKey, this.customField).subscribe(resp => {
          this.toastr.success('Custom Field', 'removed');
          this.cancel();
          this.getFields();
        });
      }
    });
  }

}
