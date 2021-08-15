import { Project } from './../project-manage/project';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpService } from '../http.service';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthenticationService } from './../../auth/authentication.service';

@Injectable()
export class DashboardService {
  currentProject: Project;
  currentProjectKey: string;
  currentBoard = undefined;
  homeView = false;
  projects = [];
  constructor(private http: HttpClient, public route: ActivatedRoute, private router: Router,
    public authenticationService: AuthenticationService) {
    this.generateMenu();
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        if (event.urlAfterRedirects != '/login')
          this.generateMenu();
      });
  }

  generateMenu() {
    if (this.route.children.length > 0 && this.route.snapshot.children[0] && this.route.snapshot.children[0].children[0]
      && this.route.snapshot.children[0].children[0].paramMap.keys.length > 0) {
      this.homeView = (this.route.snapshot.children[0].children[0].paramMap.keys.length === 0);
      const params = this.route.snapshot.children[0].children[0].params;
      if (params['projectKey'] !== undefined) {
        this.currentProjectKey = params['projectKey'];
      }
      if (params['issueKeyPair'] !== undefined) {
        const issueKeyPair = params['issueKeyPair'];
        this.currentProjectKey = issueKeyPair.substr(0, issueKeyPair.indexOf('-'));
      }
      this.homeView = false;
    } else {
      this.homeView = true;
    }
    this.calcCurrentProject();
    setTimeout(() => {
      this.calcCurrentProject();
    }, 1000);
  }

  calcCurrentProject() {
    if (this.authenticationService.projects) {
      if (this.authenticationService.projects.length > 0) {
        this.currentProject = this.authenticationService.projects[0];
      }
      this.authenticationService.projects.forEach(p => {
        if (this.currentProjectKey && p.key === this.currentProjectKey) {
          this.currentProject = p;
        }
      });
    }
    if (this.currentProject) {
      this.currentBoard = this.getBoardForUser(this.currentProject.key);
    }
  }

  getProjectOverview() {
    return this.http.get<any>(HttpService.getBaseURL() + '/project/overview');
  }

  getAssignedToMe() {
    return this.http.get<any>(HttpService.getBaseURL() + '/issue/assignedToMe');
  }

  getDue(days: number) {
    return this.http.get<any>(HttpService.getBaseURL() + '/issue/due?days=' + days);
  }

  weekLogOverview() {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/workLog');
  }

  getBoardForUser(projectKey: string) {
    let boardData: any = localStorage.getItem('JD-B');
    if (boardData) {
      boardData = JSON.parse(boardData);
      return boardData[projectKey];
    }
    return 0;
  }

  setBoardForUser(projectKey: string, boardId: number) {
    let boardData: any = localStorage.getItem('JD-B');
    if (!boardData) {
      boardData = {};
    } else {
      boardData = JSON.parse(boardData);
    }
    boardData[projectKey] = boardId;
    localStorage.setItem('JD-B', JSON.stringify(boardData));
  }

}
