import { IssueStatus } from './workflow-step';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { IssueType } from './issue-type';
import { HttpService } from '../http.service';

@Injectable()
export class IssueTypeService {
  issueTypes: IssueType[];
  issueType: IssueType;
  issueStatus: IssueStatus[];
  constructor(private http: HttpClient) { }

  getIssueTypes(projectKey: string) {
    this.http.get<IssueType[]>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/issue_type/?include_inactive=true')
      .subscribe(resp => {
        this.issueTypes = resp;
        return this.issueTypes;
      }, error => {
        console.log(error);
      }, () => { console.log('Finish loading all IssueTypes'); });
  }

  getIssueStatuses() {
    this.http.get<IssueStatus[]>(HttpService.getBaseURL() + '/manage/project/0/issue_type/statuses/')
      .subscribe(resp => {
        this.issueStatus = resp;
        return this.issueStatus;
      }, error => {
        console.log(error);
      }, () => { console.log('Finish loading all issueStatus'); });
  }

  getIssueTypesObservable(projectKey: string) {
    return this.http.get<IssueType[]>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/issue_type/');
  }

  getIssueType(projectKey: string, id: number) {
    return this.http.get<IssueType>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/issue_type/' + id + '/');
  }

  createIssueType(projectKey: string, issueType: IssueType) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/issue_type/create/', issueType);
  }

  save(projectKey: string, issueType: IssueType) {
    return this.http.put<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/issue_type/update/' + issueType.id,
      issueType);
  }

  update(projectKey: string, id: number, field: string, member: string) {
    return this.http.put<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/issue_type/' + id +
      '/update/?field=' + field + '&value=' + member, '');
  }

  closeIssueType() {
    this.issueType = undefined;
  }

  fetchIssueType() {
    return this.issueType;
  }

  changeWorkflow(projectKey: string, id: number, workflowID: number) {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/project/' +
      projectKey + '/issue_type/change/' + id + '/workflow/' + workflowID);
  }

}
