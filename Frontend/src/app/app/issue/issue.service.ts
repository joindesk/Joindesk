import { CustomField } from './../project-manage/custom-field/custom-field';
import { IssueType } from './../project-manage/issue-type';
import { JDUser } from './../admin/user/user';
import { Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Issue, FileProgress, Comment, IssueFilterDTO, Attachment, History, IssueRelationship,
  Relationship, FilteredIssues, WorkLog, IssueMentions, Label, IssueFilter, Task, IssueOtherRelationship
} from './issue';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Workflow } from '../project-manage/workflow';
import * as Stomp from 'stompjs';
import * as SockJS from 'sockjs-client';
import { HttpService } from '../http.service';

@Injectable()
export class IssueService {
  stompClient: any;
  public issueNotification = new Subject<any>();
  public issueCreateNotification = new Subject<Issue>();
  public attachmentNotification = new Subject<FileProgress>();
  constructor(private http: HttpClient) {
    this.wsConnect();
  }

  wsConnect() {
    let ws = new SockJS(HttpService.getBaseURL() + "/ws");
    this.stompClient = Stomp.over(ws);
    this.stompClient.debug = null
    const _this = this;
    this.stompClient.connect({}, (frame) => {
      _this.stompClient.subscribe("/topic/issue", (sdkEvent) => {
        this.issueNotification.next(sdkEvent.body);
      });
      _this.stompClient.reconnect_delay = 2000;
    }, (error => {
      console.log('WS: ' + error);
      setTimeout(() => {
        this.wsConnect();
      }, 15000);
      console.log('WS: Reconecting in 15 seconds');
    }));
  }

  getIssues(projectKey: string, filter: IssueFilterDTO) {
    return this.http.post<FilteredIssues>(HttpService.getBaseURL() + '/issue/' + projectKey + '/', filter);
  }

  getLabels(projectKey: string, q: string) {
    return this.http.get<Label[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/labels?q=' + q);
  }

  addLabel(projectKey: string, label: string) {
    return this.http.get<Label>(HttpService.getBaseURL() + '/issue/' + projectKey + '/addLabel?q=' + label);
  }

  findIssues(filter: IssueFilterDTO) {
    return this.http.post<Issue[]>(HttpService.getBaseURL() + '/issue/find/', filter);
  }

  public exportIssues(projectKey: string, filter: FilteredIssues, type: string) {
    return this.http.post(HttpService.getBaseURL() + '/issue/' + projectKey + '/export?type=' + type, filter,
      { responseType: 'blob' });
  }

  getBaseFilter(projectKey: string, filterId: number) {
    return this.http.get<IssueFilterDTO>(HttpService.getBaseURL() + '/issue/?p=' + projectKey + '&f=' + filterId);
  }

  getFilters(projectKey: string) {
    return this.http.get<IssueFilter[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/filters');
  }

  getOpenFilters(projectKey: string) {
    return this.http.get<IssueFilter[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/filters/open');
  }

  getIssue(projectKey: string, id: number) {
    return this.http.get<Issue>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/');
  }

  getIssuesBetween(projectKey: string, getStart: any, getEnd: any) {
    return this.http.get<FilteredIssues>(HttpService.getBaseURL() + '/issue/' + projectKey + '/searchBetween?from=' + getStart + '&to=' + getEnd);
  }

  searchIssues(projectKey: string, q: string) {
    return this.http.get<Issue[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/search?q=' + q);
  }

  searchLIssues(projectKey: string, searchFilter: FilteredIssues) {
    return this.http.post<FilteredIssues>(HttpService.getBaseURL() + '/issue/' + projectKey + '/searchl', searchFilter);
  }

  saveFilter(projectKey: string, filter: IssueFilter) {
    return this.http.post<IssueFilter>(HttpService.getBaseURL() + '/issue/' + projectKey + '/filter/save', filter);
  }

  getComments(projectKey: string, id: number) {
    return this.http.get<Comment[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/comments/');
  }

  comment(projectKey: string, issue: Issue, comment: Comment) {
    return this.http.post<Comment>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/comment/',
      comment);
  }

  deleteComment(projectKey: string, issue: Issue, comment: Comment) {
    return this.http.post<Comment>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/comment/delete',
      comment);
  }

  getTasks(projectKey: string, id: number) {
    return this.http.get<Task[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/task/');
  }

  saveTask(projectKey: string, issue: Issue, task: Task) {
    return this.http.post<Task>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/task/',
      task);
  }

  completeTask(projectKey: string, issue: Issue, task: Task) {
    return this.http.put<Task>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/task/complete',
      task);
  }

  reorderTasks(projectKey: string, issue: Issue, tasks: Task[]) {
    return this.http.put<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/task/reorder',
      tasks);
  }

  deleteTask(projectKey: string, issue: Issue, task: Task) {
    return this.http.post<Task>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/task/delete',
      task);
  }

  getWorkflow(projectKey: string, id: number) {
    return this.http.get<Workflow>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/workflow/');
  }

  getAttachments(projectKey: string, id: number) {
    return this.http.get<Attachment[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/attachments/');
  }

  getRelationshipTypes() {
    return this.http.get<Relationship[]>(HttpService.getBaseURL() + '/issue/relationship_types/');
  }

  getIssueRelationships(projectKey: string, id: number) {
    return this.http.get<IssueRelationship[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/relationships/');
  }

  addIssueRelationship(projectKey: string, issue: Issue, issueRelationship: IssueRelationship) {
    return this.http.post<IssueRelationship>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/relationships/add',
      issueRelationship);
  }

  deleteIssueRelationship(projectKey: string, issue: Issue, issueRelationship: IssueRelationship) {
    return this.http.post<IssueRelationship>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/relationships/remove',
      issueRelationship);
  }

  getIssueOtherRelationships(projectKey: string, id: number) {
    return this.http.get<IssueOtherRelationship[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/o_relationships/');
  }

  addIssueOtherRelationship(projectKey: string, issue: Issue, issueRelationship: IssueOtherRelationship) {
    return this.http.post<IssueRelationship>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/o_relationships/add',
      issueRelationship);
  }

  deleteIssueOtherRelationship(projectKey: string, issue: Issue, issueRelationship: IssueOtherRelationship) {
    return this.http.post<IssueRelationship>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/o_relationships/remove',
      issueRelationship);
  }

  getIssueMentions(projectKey: string, id: number) {
    return this.http.get<IssueMentions[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/mentions/');
  }

  getHistory(projectKey: string, id: number) {
    return this.http.get<History[]>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + id + '/history/');
  }

  createIssue(projectID: number, issue: Issue) {
    return this.http.post<any>(HttpService.getBaseURL() + '/issue/' + projectID + '/add/', issue);
  }

  save(projectKey: string, issue: Issue) {
    return this.http.put<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/update/',
      issue);
  }

  quickUpdate(data: any) {
    return this.http.put<any>(HttpService.getBaseURL() + '/issue/quick_update/',
      data);
  }

  saveCustomField(projectKey: string, issueKey: string, customField: CustomField) {
    return this.http.put<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issueKey + '/updateCustomField/',
      customField);
  }

  delete(projectKey: string, issue: Issue) {
    return this.http.delete<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/delete/');
  }

  changeType(projectKey: string, issueKey: string, issueType: IssueType) {
    return this.http.put<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issueKey + '/change_type/',
      issueType);
  }

  transition(projectKey: string, issue: Issue, payload: any, transitionID: number) {
    return this.http.put<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/transition/' + transitionID,
      payload);
  }

  laneTransition(projectKey: string, issueKey: number, laneID: number) {
    return this.http.get(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issueKey + '/lane_transition/' + laneID);
  }

  upload(projectKey: string, issueKey: string, formData: FormData) {
    return this.http.post<any>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issueKey + '/attach',
      formData);
  }

  public attach(issue: Issue, files) {

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      // create a new multipart-form for every file
      const formData: FormData = new FormData();
      formData.append('file', file, file.name);

      const status = new FileProgress();
      // Save every progress-observable in a map of all observables
      status.file = file.name;
      status.issueKey = issue.key;

      status.status = 'Started';
      this.attachmentNotification.next(status);

      // send the http-request and subscribe for progress-updates
      this.upload(issue.project.key, issue.key, formData).subscribe(resp => {
        status.status = 'Done';
        status.attachment = resp;
        this.attachmentNotification.next(status);
      });
    }
  }

  public attachDesc(projectKey: string, issueKey: string, files) {
    const file = files[0];
    // create a new multipart-form for every file
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);

    // send the http-request and subscribe for progress-updates
    return this.upload(projectKey, issueKey, formData).pipe(map(resp => {
      return resp;
    }));
  }

  public getAttachment(projectKey: string, issueKey: string, attachmentID: number) {
    return this.http.get(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issueKey + '/attachment/' + attachmentID,
      { responseType: 'blob' });
  }

  deleteAttachment(projectKey: string, issue: Issue, a: Attachment) {
    return this.http.post<Attachment>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/detach',
      a);
  }

  addWatcher(projectKey: string, issue: Issue, watcher: JDUser) {
    return this.http.post<IssueRelationship>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/watcher/add',
      watcher);
  }

  removeWatcher(projectKey: string, issue: Issue, watcher: JDUser) {
    return this.http.post<IssueRelationship>(HttpService.getBaseURL() + '/issue/' + projectKey + '/' + issue.key + '/watcher/remove',
      watcher);
  }

  /* Work Log */

  getWorkLogs(projectKey: string, issueID: number) {
    return this.http.get<WorkLog[]>(HttpService.getBaseURL() + '/worklog/' + projectKey + '/' + issueID + '/');
  }

  getWorkLog(projectKey: string, issueID: number, workLogID: number) {
    return this.http.get<WorkLog>(HttpService.getBaseURL() + '/worklog/' + projectKey + '/' + issueID + '/log/' + workLogID);
  }

  saveWorkLog(projectKey: string, issueID: number, worklog: WorkLog) {
    return this.http.post<WorkLog>(HttpService.getBaseURL() + '/worklog/' + projectKey + '/' + issueID + '/log/',
      worklog);
  }

  removeWorkLog(projectKey: string, issueID: number, workLogID: number) {
    return this.http.delete(HttpService.getBaseURL() + '/worklog/' + projectKey + '/' + issueID + '/log/' + workLogID);
  }

  /* Git repo */
  getBranches(projectKey: string, issueKey: number) {
    return this.http.get<any[]>(HttpService.getBaseURL() + '/project/' + projectKey + '/git/repo/branches?id=' + issueKey);
  }

  getCommits(projectKey: string, issueKey: number) {
    return this.http.get<any[]>(HttpService.getBaseURL() + '/project/' + projectKey + '/git/repo/commits?id=' + issueKey);
  }

}
