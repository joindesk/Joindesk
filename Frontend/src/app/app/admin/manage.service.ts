import { WebHook } from './../project-manage/webhook';
import { Access } from './../project-manage/access';
import { Group } from './../project-manage/group';
import { Resolution, Relationship } from './../issue/issue';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpService } from '../http.service';

@Injectable()
export class ManageService {
  constructor(private http: HttpClient) { }

  getResolutions() {
    return this.http.get<Resolution[]>(HttpService.getBaseURL() + '/manage/resolutions/');
  }

  saveResolution(resolution: Resolution) {
    return this.http.post<Resolution>(HttpService.getBaseURL() + '/manage/resolutions/save/',
      resolution);
  }

  removeResolution(projectKey: string, issueID: number, ResolutionID: number) {
    return this.http.delete(HttpService.getBaseURL() + '/Resolution/' + projectKey + '/' + issueID + '/log/');
  }

  getRelationships() {
    return this.http.get<Relationship[]>(HttpService.getBaseURL() + '/manage/relationships/');
  }

  saveRelationship(relationship: Relationship) {
    return this.http.post<Relationship>(HttpService.getBaseURL() + '/manage/relationships/save/',
      relationship);
  }

  removeRelationship(relationship: Relationship) {
    return this.http.post<Relationship>(HttpService.getBaseURL() + '/manage/relationships/remove/',
      relationship);
  }

  getGlobalGroups() {
    return this.http.get<Group[]>(HttpService.getBaseURL() + '/manage/group/');
  }

  saveGlobalGroup(group: Group) {
    return this.http.post<Group>(HttpService.getBaseURL() + '/manage/group/save/',
      group);
  }

  removeGlobalGroup(group: Group) {
    return this.http.post<Group>(HttpService.getBaseURL() + '/manage/group/remove/',
      group);
  }

  // IP Filtering
  getAccess() {
    return this.http.get<Access[]>(HttpService.getBaseURL() + '/manage/access/');
  }

  saveAccess(access: Access) {
    return this.http.post<Access>(HttpService.getBaseURL() + '/manage/access/save/',
      access);
  }

  removeAccess(access: Access) {
    return this.http.post<Access>(HttpService.getBaseURL() + '/manage/access/delete/',
      access);
  }

  // App configurations
  getAppConfig() {
    return this.http.get<any>(HttpService.getBaseURL() + '/app/config/');
  }

  saveAppConfig(config: any) {
    return this.http.post<Group>(HttpService.getBaseURL() + '/app/config/',
      config);
  }

  // Web Hook
  getWebHooks() {
    return this.http.get<WebHook[]>(HttpService.getBaseURL() + '/manage/webhook/');
  }

  saveWebHook(webHook: WebHook) {
    return this.http.post<WebHook>(HttpService.getBaseURL() + '/manage/webhook/save/',
      webHook);
  }

  removeWebHook(webHook: WebHook) {
    return this.http.post<WebHook>(HttpService.getBaseURL() + '/manage/webhook/delete/',
      webHook);
  }

  getEventTypes() {
    return this.http.get<string[]>(HttpService.getBaseURL() + '/manage/webhook/eventTypes');
  }

}
