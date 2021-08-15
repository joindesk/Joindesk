import { JDToastrService } from './../../toastr.service';
import { Issue, IssueFilterDTO, FilteredIssues, IssueFilter } from './../../issue/issue';
import { DashboardService } from './../dashboard.service';
import { AuthenticationService } from './../../../auth/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, TemplateRef, OnDestroy, ViewChildren, QueryList, HostListener, ElementRef } from '@angular/core';
import { IssueService } from '../../issue/issue.service';
import { NzNotificationService, NzNotificationDataFilled, NzTableComponent, NzPopoverDirective } from 'ng-zorro-antd';
import { IssueModalService } from '../../issue/issue-modal.service';

@Component({
  selector: 'app-dashboard-home',
  templateUrl: './dashboard-home.component.html',
  styleUrls: ['./dashboard-home.component.css']
})
export class DashboardHomeComponent implements OnInit, OnDestroy {
  data: FilteredIssues;
  filter: IssueFilterDTO;
  filterCopy: IssueFilterDTO;
  loadedFilter: IssueFilterDTO;
  filters: IssueFilter[];
  objectKeys = Object.keys;
  filterId: number;
  currentPage = 1;
  queryFilter;
  currentPageSize = 10;
  setQuery = '';
  loading = false;
  filterDrawerVisible = false;
  pageSizeOptions: number[] = [10, 25, 50, 100];
  selectedFilters: [];
  customize = {
    compact: true,
    columns: [
      { "field": "status", "label": "Status", "display": true },
      { "field": "priority", "label": "Priority", "display": true },
      { "field": "assignee", "label": "Assignee", "display": true },
      { "field": "reporter", "label": "Reporter", "display": true },
      { "field": "due_date", "label": "Due Date", "display": true },
      { "field": "start_date", "label": "Start", "display": false },
      { "field": "end_date", "label": "End", "display": false },
      { "field": "created", "label": "Created", "display": true },
      { "field": "updated", "label": "Updated", "display": true },
      { "field": "resolution", "label": "Resolution", "display": false },
      { "field": "resolved", "label": "Resolved", "display": false },
      { "field": "version", "label": "Version", "display": false }
    ]
  }

  config: QueryBuilderConfig = {
    fields: {}
  }
  issueKeys = [];
  updateNotification: NzNotificationDataFilled;
  showFilterSaveModal = false;
  showFilterEditModal = false;
  showAllFiltersModal = false;
  filterSaveModel = {
    type: "new",
    name: undefined,
    filter: undefined,
    open: false
  };

  @ViewChildren(NzPopoverDirective) popoverDirectives: QueryList<NzPopoverDirective>;
  @ViewChild('issuesTable', { static: false }) issuesTable: NzTableComponent<{}>;
  @ViewChild('updatedNotificationTemplate', { static: false }) updatedNotificationTemplate: TemplateRef<{}>;
  constructor(private authenticationService: AuthenticationService, private dashboardService: DashboardService,
    private toastr: JDToastrService, private eRef: ElementRef,
    public route: ActivatedRoute, private router: Router, private issueService: IssueService, private notification: NzNotificationService,
    private issueModalService: IssueModalService) { }

  ngOnInit() {
    if (localStorage.getItem('JD-CZ')) {
      this.customize = JSON.parse(localStorage.getItem('JD-CZ'));
    }
    this.loading = true;
    this.route.queryParams.subscribe(qParams => {
      let filterChanged = false;
      if (this.filterId != qParams["f"])
        filterChanged = true;
      this.filterId = qParams["f"];
      this.filterId = this.filterId == undefined ? 0 : this.filterId;
      this.currentPage = qParams["p"];
      this.currentPage = this.currentPage == undefined ? 1 : this.currentPage;
      this.currentPageSize = qParams["ps"];
      this.currentPageSize = this.currentPageSize == undefined ? 10 : this.currentPageSize;
      this.setQuery = qParams["q"];
      this.queryFilter = qParams["filter"];
      if (filterChanged)
        this.loadFilter();
      else
        this.get();
    });
    this.route.params.subscribe(params => {
      this.dashboardService.currentProjectKey = params['projectKey'];
      this.loadFilter();
      this.getFilters();
    });
    this.issueService.issueNotification.subscribe(resp => {
      resp = JSON.parse(resp);
      if (this.dashboardService.currentProjectKey && resp["project"] == this.dashboardService.currentProjectKey && this.issueKeys)
        this.issueKeys.filter(i => (resp["project"] + "-" + i[0]) == resp["issue"]).forEach(i => {
          if (i[1] != resp["updated"]) {
            if (this.updateNotification)
              this.notification.remove(this.updateNotification.messageId);
            this.updateNotification = this.notification.template(this.updatedNotificationTemplate, { nzDuration: 0, nzKey: 'resultUpdated' });
          }
        })
    })
  }

  ngOnDestroy() {
    if (this.updateNotification)
      this.notification.remove(this.updateNotification.messageId);
  }

  customizeEvent(c) {
    this.customize = c;
  }

  getFilter(id) {
    var v = ' - Unknown filter - ';
    this.filters.forEach(f => {
      if (id == f.id) {
        v = f.name;
      }
    });
    return v;
  }

  setFilter(fID: number) {
    this.data.pageIndex = 1;
    this.router.navigate([], {
      queryParams: {
        f: fID,
        filter: undefined,
        p: this.data.pageIndex,
        ps: this.data.pageSize
      },
      queryParamsHandling: 'merge',
    });
  }

  loadFilter() {
    this.loading = true;
    this.issueService.getBaseFilter(this.dashboardService.currentProjectKey, this.filterId).subscribe(resp => {
      this.filter = resp;
      this.filterCopy = JSON.parse(JSON.stringify(this.filter));
      let fields = {};
      //this.filter.filters.sort((a, b) => a.label.localeCompare(b.label)).forEach(fltr => {
      this.filter.filters.forEach(fltr => {
        if (fltr.options.length > 0 || fltr.type == 'date')
          fields[fltr.field] = {
            name: fltr.label,
            type: fltr.type,
            operators: fltr.operators,
            options: fltr.options
          };
      });
      const c: QueryBuilderConfig = {
        fields: fields
      };
      this.config = JSON.parse(JSON.stringify(c));
      this.data = new FilteredIssues();
      this.data.filter = this.filter.filter;
      this.data.pageIndex = this.currentPage;
      this.data.pageSize = this.currentPageSize;
      if (!this.data.filter.id && this.setQuery != undefined) {
        // set rules if q from url
        this.setQuery.split(";").forEach(q => {
          if (q.indexOf(':') > 0) {
            const f = q.substr(0, q.indexOf(':'));
            const v = q.substr(q.indexOf(':') + 1);
            this.data.filter.query.rules.push({ id: 43, field: f, operator: "IN", valueFrom: null, valueTo: null, values: [v] });
          }
        })
      } else if (this.queryFilter) {
        this.data.filter.query.rules = JSON.parse(atob(this.queryFilter))
      }
      this.expandFilterValues();
      this.loadedFilter = JSON.parse(JSON.stringify(this.filter));
      this.get();
    });
  }

  getFilters() {
    this.issueService.getFilters(this.dashboardService.currentProjectKey).subscribe(resp => {
      this.filters = resp;
    });
  }

  clearAllFilter() {
    this.setFilter(0);
    this.filter.filter.query['rules'] = [];
    this.router.navigateByUrl('/project/' + this.dashboardService.currentProjectKey);
    this.executeFilter();
  }

  @HostListener('document:click', ['$event'])
  clickout(event) {
    if (event.target.classList.contains("cdk-overlay-backdrop")) {
      this.popoverDirectives.forEach(element => {
        if (element && element.elementRef.nativeElement.classList.contains("filterRule")) {
          element.hide();
        }
      });
    }
  }

  addFilterRule(key, value) {
    this.data.pageIndex = 1;
    this.filter.filter.query.rules.push({
      field: key,
      operator: value.operators[0],
    });
    (async () => {
      await this.delay(200).then(a => {
        if (!this.popoverDirectives) return
        console.log(this.popoverDirectives.length);
        this.popoverDirectives.forEach(element => {
          if (element && element.elementRef.nativeElement.className == "filterRule"
            && parseInt(element.elementRef.nativeElement.dataset.filterindex) + 1 == this.filter.filter.query.rules.length) {
            element.show();
          }
        });
      })
    })();
  }

  delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  removeFilterRule(ruleIndex: number) {
    this.filter.filter.query.rules.splice(ruleIndex, 1);
    this.data.pageIndex = 1;
    this.executeFilter();
  }

  isFilterModified() {
    return JSON.stringify(this.filter) != JSON.stringify(this.filterCopy);
  }

  isLoadedFilterModified() {
    if (this.filterCopy && this.loadedFilter)
      return JSON.stringify(this.filterCopy.filter) != JSON.stringify(this.loadedFilter.filter);
    else
      return false;
  }

  canSaveFilter() {
    return this.isLoadedFilterModified() ||
      ((this.filterId == undefined || this.filterId <= 0) && this.filter && this.filter.filter.query.rules.length > 0);
  }

  canEditFilter() {
    return ((this.filterId != undefined && this.filterId > 0) && this.filter && this.filter.filter.owner.userName == this.authenticationService.getCurrentUser().userName);
  }

  editFilter() {
    this.filterSaveModel.name = this.filter.filter.name;
    this.filterSaveModel.open = this.filter.filter.open;
    this.showFilterEditModal = true;
  }

  saveEditFilter() {
    if (!this.filterSaveModel.name || this.filterSaveModel.name.length <= 0) {
      this.toastr.errorMessage("Filter name is required");
      return;
    }
    this.filter.filter.name = this.filterSaveModel.name;
    this.filter.filter.query = this.filterCopy.filter.query;
    this.filter.filter.open = this.filterSaveModel.open;
    this.issueService.saveFilter(this.dashboardService.currentProjectKey, this.filter.filter).subscribe(resp => {
      this.toastr.successMessage("Filter saved");
      this.data.pageIndex = 1;
      this.router.navigate([], {
        queryParams: {
          f: resp.id,
          filter: undefined,
          p: this.data.pageIndex,
          ps: this.data.pageSize
        },
        queryParamsHandling: 'merge',
      });
      this.getFilters();
    });
    this.toastr.successMessage("Saved")
    this.showFilterEditModal = false;
  }

  saveFilter() {
    if (!this.isValid()) {
      this.toastr.errorMessage("Current filter has errors");
      return;
    }
    if (this.filterSaveModel.type == 'new' && (!this.filterSaveModel.name)) {
      this.toastr.errorMessage("Filter name is required");
      return;
    }
    if (this.filterSaveModel.type == 'new' && (this.filterSaveModel.name.length <= 2 && this.filterSaveModel.name.length <= 50)) {
      this.toastr.errorMessage("Filter name should be between 2 to 50");
      return;
    }
    if (this.filterSaveModel.type == 'overwrite' && !this.filterSaveModel.filter) {
      this.toastr.errorMessage("Please select an existing filter");
      return;
    }
    if (this.filterSaveModel.type == 'overwrite' && this.filterSaveModel.filter
      && this.filterSaveModel.filter.readonly) {
      this.toastr.errorMessage("Cannot overwrite readonly filter, trying creating new");
      return;
    }
    if (this.filterSaveModel.type == 'new') {
      this.filter.filter.id = undefined;
      this.filter.filter.name = this.filterSaveModel.name;
      this.filter.filter.open = this.filterSaveModel.open;
      this.filter.filter.query.owner = undefined;
    } else {
      this.filter.filter.id = this.filterSaveModel.filter ? this.filterSaveModel.filter.id : undefined;
    }
    this.filter.filter.query.id = undefined;
    this.filter.filter.query.rules.forEach(rules => {
      rules.id = undefined;
    });
    this.issueService.saveFilter(this.dashboardService.currentProjectKey, this.filter.filter).subscribe(resp => {
      this.toastr.successMessage("Filter saved");
      this.data.pageIndex = 1;
      this.router.navigate([], {
        queryParams: {
          f: resp.id,
          filter: undefined,
          p: this.data.pageIndex,
          ps: this.data.pageSize
        },
        queryParamsHandling: 'merge',
      });
      this.getFilters();
    });
    this.toastr.successMessage("Saved")
    this.showFilterSaveModal = false;
  }

  isValid() {
    let valid = true;
    this.filter.filter.query.rules.forEach(filter => {
      if ((filter.operator == "IN" || filter.operator == "NOT IN") && (!filter.values || filter.values.length <= 0 || filter.values[0] == undefined)) {
        this.toastr.error(filter.field, "Invalid filter criteria");
        valid = false;
      }
      if ((filter.operator == "CUSTOM") && ((!filter.valueFrom && !filter.valueTo) || (filter.valueFrom == undefined && filter.valueTo == undefined))) {
        this.toastr.error(filter.field, "Invalid filter criteria");
        valid = false;
      }
    });
    return valid;
  }

  filterChangeEvent() {
    this.expandFilterValues()
  }

  expandFilterValues() {
    this.filter.filter.query.rules.forEach(rule => {
      let expandedValues = [];
      if (rule.values) {
        rule.values.forEach(v => {
          this.config.fields[rule.field].options.forEach(o => {
            if (o.value == v) {
              expandedValues.push(o.name)
            }
          });
        });
      }
      rule.expandedValues = expandedValues;
    });
  }

  executeSearch() {
    this.filter.filter.searchQuery = this.filter.filter.searchQuery.trim();
    if (this.isFilterModified()) {
      this.executeFilter();
    } else
      this.toastr.errorMessage("No filter changes detected")
  }

  executeFilter() {
    if (this.isValid()) {
      //this.filter = JSON.parse(JSON.stringify(this.filterCopy));
      this.searchQuery();
      this.filterDrawerVisible = false;
    }
  }

  searchQuery() {
    this.data = new FilteredIssues();
    this.data.filter = this.filter.filter;
    this.get();
  }

  copy(inputElement) {
    inputElement.select();
    document.execCommand('copy');
    inputElement.setSelectionRange(0, 0);
    this.toastr.info('Query copied to clipboard', 'Copied');
  }

  openIssue(keyPair) {
    console.log(keyPair);
    //this.router.navigateByUrl('/issue/' + $event.event.id);
    this.issueModalService.openIssueModal(keyPair);
  }

  updateColumn(col) {
    if (typeof col == 'string')
      this.customize[col] = !this.customize[col];
    else
      col.display = !col.display;
    localStorage.setItem('JD-CZ', JSON.stringify(this.customize));
  }

  get() {
    if (this.data) {
      this.data.timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
      this.loading = true;
      this.issueService.searchLIssues(this.dashboardService.currentProjectKey, this.data).subscribe(resp2 => {
        this.data = resp2;
        this.issueKeys = resp2['issueKeys'];
        this.filter.filter.query = this.data.filter.query;
        this.filterCopy = JSON.parse(JSON.stringify(this.filter));
        if (this.filterId && this.loadedFilter && this.isLoadedFilterModified()) {
          this.filterId = undefined
          this.loadedFilter = undefined
        }
        this.router.navigate([], {
          queryParams: {
            f: this.filterId,
            p: this.data.pageIndex,
            filter: (this.filter.filter.query.rules.length > 0) ? btoa(JSON.stringify(this.filter.filter.query.rules)) : undefined
          },
          queryParamsHandling: 'merge'
        });
        this.loading = false;
        if (this.updateNotification)
          this.notification.remove(this.updateNotification.messageId);
      });
    }
  }

  compareFn(user1: any, user2: any) {
    return user1 && user2 ? user1.id === user2.id : user1 === user2;
  }

  export(type: string) {
    this.data.timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.issueService.exportIssues(this.dashboardService.currentProjectKey, this.data, type).subscribe((res: Blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(res);
      let fileName = 'export_' + Date.now();
      if (type === 'csv') { fileName += '.csv'; } else { fileName += '.xls'; }
      a.download = fileName;
      a.click();
    });
  }

  paginate(val) {
    this.data.pageSize = this.currentPageSize;
    this.data.pageIndex = val;
    this.router.navigate([], {
      queryParams: {
        p: this.data.pageIndex,
        ps: this.data.pageSize
      },
      queryParamsHandling: 'merge',
    });
    //this.get();
  }

  pageSizeChange(val) {
    this.currentPageSize = val;
    this.data.pageSize = val;
    this.data.pageIndex = 1;
    this.router.navigate([], {
      queryParams: {
        p: this.data.pageIndex,
        ps: this.data.pageSize
      },
      queryParamsHandling: 'merge',
    });
    //this.get();
  }
}

export interface QueryBuilderConfig {
  fields: FieldMap;
  entities?: EntityMap;
  allowEmptyRulesets?: boolean;
  getOperators?: (fieldName: string, field: Field) => string[];
  getInputType?: (field: string, operator: string) => string;
  getOptions?: (field: string) => Option[];
  addRuleSet?: (parent: RuleSet) => void;
  addRule?: (parent: RuleSet) => void;
  removeRuleSet?: (ruleset: RuleSet, parent: RuleSet) => void;
  removeRule?: (rule: Rule, parent: RuleSet) => void;
  coerceValueForOperator?: (operator: string, value: any, rule: Rule) => any;
}

export interface EntityMap {
  [key: string]: Entity;
}
export interface Entity {
  name: string;
  value?: string;
  defaultField?: any;
}

export interface Rule {
  field: string;
  value?: any;
  operator?: string;
  entity?: string;
}
export interface Option {
  name: string;
  value: any;
}
export interface FieldMap {
  [key: string]: Field;
}
export interface Field {
  name: string;
  value?: string;
  type: string;
  nullable?: boolean;
  options?: Option[];
  operators?: string[];
  defaultValue?: any;
  defaultOperator?: any;
  entity?: string;
  validator?: (rule: Rule, parent: RuleSet) => any | null;
}

export interface RuleSet {
  condition: string;
  rules: Array<RuleSet | Rule>;
  collapsed?: boolean;
}