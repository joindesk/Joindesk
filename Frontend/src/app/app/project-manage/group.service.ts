import { JDUser } from '../admin/user/user';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Group } from './group';
import { HttpService } from '../http.service';

@Injectable()
export class GroupService {
  group: Group;
  constructor(private http: HttpClient) { }

  getGroups(projectKey: string) {
    return this.http.get<Group[]>(HttpService.getBaseURL() + '/project/' + projectKey + '/group/');
  }

  getGroup(projectKey: string, id: number) {
    return this.http.get<Group>(HttpService.getBaseURL() + '/project/' + projectKey + '/group/' + id + '/');
  }

  getAuthorityCodes() {
    return this.http.get<string[]>(HttpService.getBaseURL() + '/project/manage/authoritycodes/');
  }

  getMembers() {
    return this.http.get<JDUser[]>(HttpService.getBaseURL() + '/project/manage/members/');
  }

  createGroup(projectKey: string, group: Group) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/group/create/', group);
  }

  save(projectKey: string, group: Group) {
    return this.http.put<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/group/' + group.id + '/updategroup/',
      group);
  }

  remove(projectKey: string, group: Group) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/group/' + group.id + '/remove/',
      group);
  }

  update(projectKey: string, id: number, field: string, member: string) {
    return this.http.put<any>(HttpService.getBaseURL() + '/project/' + projectKey + '/group/' + id +
      '/update/?field=' + field + '&value=' + member, '');
  }

  closeGroup() {
    this.group = undefined;
  }

  fetchGroup() {
    return this.group;
  }

}
