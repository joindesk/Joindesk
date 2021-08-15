import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { NzContextMenuService, NzDropdownMenuComponent } from 'ng-zorro-antd';
import { Issue } from '../../issue/issue';
import { IssueService } from '../../issue/issue.service';
import { JDToastrService } from '../../toastr.service';
@Component({
  selector: 'app-issue-list',
  templateUrl: './issue-list.component.html'
})
export class IssueListComponent implements OnInit {
  isAllDisplayDataChecked = false;
  isIndeterminate = false;
  listOfDisplayData: Issue[] = [];
  listOfAllData: Issue[] = [];
  mapOfCheckedId: { [key: string]: boolean } = {};
  currentSelected = "";
  @Input() data;
  @Input() filter;
  @Input() loading;
  @Input() customize;
  @Input() pageSizeOptions;
  @Output() paginateEvent = new EventEmitter<any>();
  @Output() pageSizeChangeEvent = new EventEmitter<any>();
  @Output() customizeEvent = new EventEmitter<any>();
  @Output() searchEvent = new EventEmitter<any>();
  @Output() openIssueEvent = new EventEmitter<any>();
  constructor(private issueService: IssueService, private nzContextMenuService: NzContextMenuService, private toastr: JDToastrService) { }

  ngOnInit() {
  }

  refreshStatus(): void {
    this.isAllDisplayDataChecked = this.listOfDisplayData.every(item => this.mapOfCheckedId[item.keyPair]);
    this.isIndeterminate =
      this.listOfDisplayData.some(item => this.mapOfCheckedId[item.keyPair]) && !this.isAllDisplayDataChecked;
  }

  check(keyPair, $event) {
    if (!$event.ctrlKey)
      this.checkAll(false);
    this.mapOfCheckedId[keyPair] = !this.mapOfCheckedId[keyPair];
  }

  checkAll(value: boolean): void {
    this.listOfDisplayData.forEach(item => (this.mapOfCheckedId[item.keyPair] = value));
    this.refreshStatus();
  }

  currentPageDataChange($event: Issue[]): void {
    this.mapOfCheckedId = {};
    this.listOfDisplayData = $event;
    this.refreshStatus();
  }

  contextMenu($event: MouseEvent, menu: NzDropdownMenuComponent, i: Issue): void {
    this.mapOfCheckedId[i.keyPair] = true;
    var selected = 0;
    for (let key in this.mapOfCheckedId) {
      if (this.mapOfCheckedId[key]) selected++;
    }
    if (selected > 0) {
      this.currentSelected = (selected == 1) ? i.keyPair : selected + " Selected";
      this.nzContextMenuService.create($event, menu);
    }
  }

  isDue(date) {
    return new Date(date) < new Date();
  }

  closeMenu(): void {
    this.toastr.infoMessage("Closed");
    this.nzContextMenuService.close();
  }

  openIssue(i) {
    console.log(i);
    this.openIssueEvent.emit(i);
  }

  quick(field, data) {
    const issuesKeyPairs = [];
    for (let key in this.mapOfCheckedId) {
      if (this.mapOfCheckedId[key]) issuesKeyPairs.push(key);
    }
    this.issueService.quickUpdate({ field: field, data: data, issuesKeyPairs: issuesKeyPairs }).subscribe(resp => {
      if (resp.length > 0)
        this.toastr.errorMessage(resp);
      else
        this.toastr.successMessage(field + " updated");
      this.searchEvent.emit('search');
    });
  }

  showColumn(colName: string) {
    return this.customize.columns.some(function (col) { return col.field == colName && col.display });
  }

  sort(sortKey): void {
    this.filter.filter.sortBy = sortKey;
    this.filter.filter.sortOrder = this.filter.filter.sortOrder == 'DESC' ? 'ASC' : 'DESC';
    this.searchEvent.emit('search');
  }

  getSort(field: string) {
    if (this.filter.filter.sortBy == field) {
      return this.filter.filter.sortOrder == 'DESC' ? 'sort-descending' : 'sort-ascending';
    }
  }

  paginate(e) {
    this.paginateEvent.emit(e);
  }

  pageSizeChange(e) {
    this.pageSizeChangeEvent.emit(e);
  }
}
