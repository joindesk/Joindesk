import { JDUser } from './../admin/user/user';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Project, TimeTracking, SlackChannel, GitRepository, GitHook } from './project';
import { Version } from '../issue/issue';
import { of } from 'rxjs';
import { HttpService } from '../http.service';

@Injectable()
export class ProjectService {
  projects: Project[];
  project: Project;
  projectMembers = {};
  constructor(private http: HttpClient) { }

  getProjects() {
    this.http.get<Project[]>(HttpService.getBaseURL() + '/project/')
      .subscribe(resp => {
        this.projects = resp;
        return this.projects;
      }, error => {
        console.log(error);
      }, () => { console.log('Finish loading all projects'); });
  }

  slackEnabled() {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/user/slackStatus');
  }

  getProjectsObservable() {
    return this.http.get<Project[]>(HttpService.getBaseURL() + '/project/');
  }

  getProject(id: number) {
    return this.http.get<Project>(HttpService.getBaseURL() + '/project/' + id + '/');
  }

  getProjectByKey(key: string) {
    return this.http.get<Project>(HttpService.getBaseURL() + '/project/key/' + key + '/');
  }

  getProjectMembers(key: string) {
    if (this.projectMembers[key]) {
      return of(this.projectMembers[key]);
    } else
      return this.http.get<JDUser[]>(HttpService.getBaseURL() + '/project/' + key + '/members/');
  }

  createProject(project: Project) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/create/', project, { observe: 'response' });
  }

  save(project: Project) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/update/' + project.id, project, { observe: 'response' });
  }

  fetchProject() {
    return this.project;
  }

  getProjectTimeTrackingSettings(key: string) {
    return this.http.get<TimeTracking>(HttpService.getBaseURL() + '/project/' + key + '/timetracking');
  }

  saveProjectTimeTrackingSettings(key: string, t: TimeTracking) {
    return this.http.post<TimeTracking>(HttpService.getBaseURL() + '/project/' + key + '/timetracking/update', t);
  }

  getProjectVersions(key: string) {
    return this.http.get<Version[]>(HttpService.getBaseURL() + '/project/' + key + '/version');
  }

  getProjectVersion(key: string, id: number) {
    return this.http.get<Version>(HttpService.getBaseURL() + '/project/' + key + '/version/' + id);
  }

  saveProjectVersion(key: string, v: Version) {
    return this.http.post<Version>(HttpService.getBaseURL() + '/project/' + key + '/version/save', v);
  }

  releaseProjectVersion(key: string, v: Version) {
    return this.http.post<Version>(HttpService.getBaseURL() + '/project/' + key + '/version/release', v);
  }

  removeProjectVersion(key: string, v: Version) {
    return this.http.post<Version>(HttpService.getBaseURL() + '/project/' + key + '/version/remove', v);
  }

  getChannels() {
    return this.http.get<SlackChannel[]>(HttpService.getBaseURL() + '/project/channels');
  }

  getProjectRepositorys(key: string) {
    return this.http.get<GitRepository[]>(HttpService.getBaseURL() + '/project/' + key + '/git/repo');
  }

  getProjectRepositoryHooks(key: string, repoId: number) {
    return this.http.get<GitHook[]>(HttpService.getBaseURL() + '/project/' + key + '/git/repo/' + repoId + '/hook');
  }

  getProjectRepositoryHook(key: string, repoId: number, id: number) {
    return this.http.get<GitHook>(HttpService.getBaseURL() + '/project/' + key + '/git/repo/' + repoId + '/hook' + id);
  }

  saveProjectGitRepository(key: string, v: GitRepository) {
    return this.http.post<GitRepository>(HttpService.getBaseURL() + '/project/' + key + '/git/repo/save', v);
  }

  saveProjectGitHook(key: string, repoId: number, v: GitHook) {
    return this.http.post<GitHook>(HttpService.getBaseURL() + '/project/' + key + '/git/repo/' + repoId + '/hook/save', v);
  }

  removeProjectGitHook(key: string, repoId: number, v: GitHook) {
    return this.http.post<any>(HttpService.getBaseURL() + '/project/' + key + '/git/repo/' + repoId + '/hook/delete', v);
  }
}
