import { Router } from '@angular/router';
import { Subject, Observable } from 'rxjs';
import { IssueTypeService } from './../../project-manage/issue-type.service';
import { ProjectService } from './../../project-manage/project.service';
import { JDUser } from './../../admin/user/user';
import { IssueService } from './../../issue/issue.service';
import { Component, OnInit, Output, Input, EventEmitter, ViewChild, ElementRef, HostListener } from '@angular/core';
import { Issue, Label } from '../../issue/issue';
import { IssueType } from '../../project-manage/issue-type';
import { IssueModalService } from '../issue-modal.service';
import { MyaccountService } from '../../myaccount/myaccountservice.service';
import { AuthenticationService } from './../../../auth/authentication.service';

@Component({
  selector: 'app-inline-edit',
  templateUrl: './inline-edit.component.html',
  styleUrls: ['./inline-edit.component.css']
})
export class InlineEditComponent implements OnInit {

  @Output()
  saveEvent: any = new EventEmitter();
  @Output()
  changeEvent: any = new EventEmitter();
  edit = false;
  pencil = false;
  currentVal; currentVals;
  showAssignToSelf = false;
  @Input() data = {
    issueKey: undefined,
    projectKey: undefined,
    field: undefined,
    value: undefined,
    renderedValue: undefined,
    values: []
  };
  @Input() editable = false;
  assignableMembers: JDUser[];
  currentUserID;
  labels: Label[];
  orgValues = undefined;
  versions = {
    released: [],
    unreleased: [],
  };
  modules: {};
  showDropdown = false;
  descEditorInstance;
  @ViewChild('fileInput', { read: true, static: false }) fileInput: ElementRef;
  @ViewChild('ctrl', { read: ElementRef, static: false }) ctrl: ElementRef;
  @Input() notifier: Subject<any>;
  @Input() mention: Subject<any>;
  constructor(private issueService: IssueService, private projectService: ProjectService, private myaccountService: MyaccountService,
    private router: Router, public issueTypeService: IssueTypeService, private ims: IssueModalService,
    private authService: AuthenticationService) { }

  ngOnInit() {
    this.projectService.getProjectMembers(this.data.projectKey).subscribe(resp => {
      resp = (resp || []).sort((a: JDUser, b: JDUser) => a.fullName < b.fullName ? -1 : 1);
      this.assignableMembers = resp;
      this.projectService.projectMembers[this.data.projectKey] = resp;
      window['members'] = resp;
      this.currentUserID = this.authService.getCurrentUser().id;
      if (this.data.field === 'assignee') {
        this.assignableMembers.forEach(m => {
          if (!this.data.value || (m.id === this.currentUserID && this.currentUserID != this.data.value.id)) {
            this.showAssignToSelf = true;
          }
        });
      }
    });
    if (this.data.field === 'description2') {
      this.data.renderedValue = this.renderData(this.data.value);
    }
    if (this.data.field === 'description') {
      this.mention.asObservable().subscribe(v => {
        if (this.descEditorInstance) {
          this.descEditorInstance.replaceSelection(v);
        }
      });
    }
    if (this.data.field === 'IssueType') {
      this.issueTypeService.getIssueTypes(this.data.projectKey);
    }
    if (this.data.field === 'label') {
      this.getLabels();
    }
    if (this.data.field === 'versions') {
      this.data.values.forEach(v => {
        if (v.released)
          this.versions.released.push(v);
        else
          this.versions.unreleased.push(v);
      });
    }
    this.currentVal = this.data.value;
    this.currentVals = this.data.values;
  }

  imageHandler = (image, callback) => {
    const event = new MouseEvent('click', { bubbles: false });
    this.fileInput.nativeElement.dispatchEvent(event);
  }

  openAttachment(name, type, fileName) {
    this.issueService.getAttachment(this.data.projectKey, this.data.issueKey, name)
      .subscribe(data => {
        const thefile = new Blob([data], { type: type });
        const url = window.URL.createObjectURL(thefile);
        // window.open(url);
        if (type === 'image/png' || type === 'image/jpg' || type === 'image/jpeg') {
          return url;
        }
        const a: HTMLAnchorElement = document.createElement('a') as HTMLAnchorElement;

        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();

        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      },
        error => console.log('Error downloading the file.'),
        () => console.log('Completed file download.'));
  }

  renderData(v) {
    const regex = /\[(attachment)\]\[(.+?)\]\[(.+?)\]\[(.+?)\]/gi;
    // v = v.replace(regex, '<button (click)="downloadAttachment(name,$2,$4)">$3</button>');
    const regex2 = /\[(image)\]\[(.+?)\]\[(.+?)\]\[(.+?)\]/gi;
    // v = v.replace(regex2, '<img src="https://www.gstatic.com/images/branding/product/1x/keep_48dp.png" />');
    return v;
  }

  saveSummary(v) {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.summary = v;
    this.updateIssue(issue, 'summary');
  }

  saveDueDate() {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.dueDate = (this.data.value) ? this.data.value.toISOString().substring(0, 10) : "1901-01-31";
    this.updateIssue(issue, 'DueDate');
  }

  saveStartDate() {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.startDate = (this.data.value) ? this.data.value.toISOString().substring(0, 10) : "1901-01-31";
    this.updateIssue(issue, 'StartDate');
  }

  saveEndDate() {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.endDate = (this.data.value) ? this.data.value.toISOString().substring(0, 10) : "1901-01-31";
    this.updateIssue(issue, 'EndDate');
  }

  descEditor($event) {
    this.descEditorInstance = $event;
  }

  saveDescription() {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.description = this.data.value;
    this.updateIssue(issue, 'description');
  }

  savePriority(val: string) {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.priority = val;
    this.updateIssue(issue, 'priority');
  }

  saveIssueType(v) {
    let issueType = new IssueType;
    issueType.id = +v;
    if (issueType.id == this.data.value.id) {
      alert('Please select different value');
    } else {
      if (confirm('This process will reset issue to start state of new type, are you sure ?')) {
        this.updateIssueType(issueType);
      }
    }
  }

  saveReporter(v) {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.reporter = new JDUser;
    issue.reporter.id = v;
    this.showDropdown = false;
    this.updateIssue(issue, 'reporter');
  }

  assignToMe() {
    this.assignableMembers.forEach(m => {
      if (m.id === this.myaccountService.user.id) {
        this.saveAssignee(m);
      }
      this.showAssignToSelf = false;
    });
  }

  saveAssignee(v) {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.assignee = new JDUser;
    issue.assignee.id = v ? v.id : 0;
    this.showDropdown = false;
    this.updateIssue(issue, 'assignee');
  }

  saveOriginalEstimate(v) {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.estimateString = v;
    this.updateIssue(issue, 'timeOriginalEstimate');
  }

  updateIssue(issue: Issue, field: string) {
    if (!this.editable) {
      alert("No Permissions to edit field");
      return;
    }
    this.changeEvent.emit(field);
    this.issueService.save(this.data.projectKey, issue).subscribe(resp => {
      this.edit = false;
      if (issue.description) {
        this.data.value = issue.description;
      }
      this.saveEvent.emit({ 'field': field, 'result': resp, 'issue': issue });
    }, err => {
      console.log(err);
    });
  }

  updateIssueType(issueType: IssueType) {
    this.changeEvent.emit('IssueType');
    this.issueService.changeType(this.data.projectKey, this.data.issueKey, issueType).subscribe(resp => {
      this.edit = false;
      this.saveEvent.emit({ 'field': 'IssueType', 'result': resp });
    });
  }

  enableEdit() {
    if (this.editable && !this.edit) {
      this.orgValues = this.data.value;
      this.edit = true; this.pencil = false;
      if (this.ctrl)
        this.ctrl.nativeElement.focus();
    }
  }

  enableDescEdit(e) {
    if (e.target && e.target.tagName == 'A') return;
    this.enableEdit();
  }

  cancelEdit() {
    this.data.value = this.orgValues;
    this.edit = false;
  }

  cancelCtrlEdit() {
    this.data.value = this.orgValues;
    this.edit = false;
    this.ctrl.nativeElement.innerText = this.data.value;
  }

  cancelDescEdit() {
    this.data.value = this.orgValues;
    this.edit = false;
  }

  isDue(date) {
    return new Date(date) < new Date();
  }

  saveVersion() {
    const issue = new Issue();
    issue.updateField = this.data.field;
    issue.key = this.data.issueKey;
    issue.versions = this.currentVal;
    this.updateIssue(issue, 'versions');
  }

  saveComponents() {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.updateField = this.data.field;
    issue.components = this.currentVal;
    this.updateIssue(issue, 'component');
  }

  getLabels() {
    this.issueService.getLabels(this.data.projectKey, '').subscribe(resp => {
      this.labels = resp;
    });
  }

  saveLabels() {
    const issue = new Issue();
    issue.key = this.data.issueKey;
    issue.updateField = this.data.field;
    issue.labels = this.currentVal;
    this.updateIssue(issue, 'label');
  }

  labelChange() {
    this.currentVal.forEach(l => {
      if (l.id == undefined) {
        this.issueService.addLabel(this.data.projectKey, l).subscribe(resp => {
          const newArr = [];
          for (let i = 0; i < this.data.value.length; ++i) {
            if (!isNaN(this.data.value[i].id) && resp.id !== this.data.value[i].id) {
              newArr.push(this.data.value[i]);
            }
          }
          this.labels.push(resp);
          this.currentVal = newArr;
        });
      }
    });
  }

  public requestAutocompleteItems = (text: string) => {
    return this.issueService.getLabels(this.data.projectKey, text);
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }

  compareCompFn(v1: any, v2: any) {
    return v1 && v2 ? v1 === v2.id : v1 === v2;
  }

  searchByLabel(v: string) {
    this.navigate('label:' + v);
  }

  searchByComponent(v: string) {
    this.navigate('component:' + v);
  }

  searchByVersion(v: string) {
    this.navigate('version:' + v);
  }

  navigate(q) {
    this.ims.closeAll();
    this.router.navigateByUrl('project/' + this.data.projectKey + '?q=' + q);
  }
}
