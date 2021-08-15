import { JDUser } from './user';
import { JDToastrService } from './../../toastr.service';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { HttpService } from '../../http.service';

@Injectable()
export class UserService {
  users: JDUser[];
  user: JDUser;
  userChange: Subject<boolean> = new Subject<boolean>();
  constructor(private http: HttpClient, private toastr: JDToastrService) { }

  getUsers() {
    this.http.get<JDUser[]>(HttpService.getBaseURL() + '/manage/user/')
      .subscribe(resp => {
        this.users = resp;
        return this.users;
      }, error => {
        console.log(error);
      }, () => { console.log('Finish loading all users'); });
  }

  getUser(id: number) {
    this.http.get<JDUser>(HttpService.getBaseURL() + '/manage/user/' + id)
      .subscribe(resp => {
        this.user = resp;
        this.user.edit = false;
        this.userChange.next(true);
      }, error => {
        console.log(error);
      }, () => { console.log('Finish loading all user' + id); });
  }

  searchUser(projectKey: string, q: string) {
    return this.http.get<JDUser[]>(HttpService.getBaseURL() + '/manage/user/search?project=' + projectKey + '&q=' + q);
  }

  createUser(user: JDUser) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/user/create', user, { observe: 'response' })
      .subscribe(resp => {
        if (resp.status === 200) {
          this.toastr.success('Success', 'User created');
          this.getUsers();
          this.user = resp.body;
          this.userChange.next(true);
        }
        return resp;
      }, (error) => {
        // tslint:disable-next-line:no-debugger
        debugger;
      });
  }

  save(user: JDUser) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/user/save', user, { observe: 'response' })
      .subscribe(resp => {
        if (resp.status === 200) {
          this.toastr.success('Success', 'User saved');
          this.user = resp.body;
          this.userChange.next(true);
        }
        return resp;
      }, (error) => {
        // tslint:disable-next-line:no-debugger
        debugger;
      });
  }

  closeUser() {
    this.user = undefined;
    this.userChange.next(true);
  }

  fetchUser() {
    return this.user;
  }

  slackEnabled() {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/user/slackStatus');
  }

  getSlackCandidates() {
    return this.http.get<any[]>(HttpService.getBaseURL() + '/manage/user/slackImportCandidate');
  }

  getPendingUsers() {
    return this.http.get<JDUser[]>(HttpService.getBaseURL() + '/manage/user/pending');
  }

  approvePendingUser(login: JDUser) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/user/pending/approve', login);
  }

  rejectPendingUser(login: JDUser) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/user/pending/reject', login);
  }

  importSlackUser(id: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/user/slackImport?id=' + id);
  }

  getApiToken(user: JDUser) {
    return this.http.post<string>(HttpService.getBaseURL() + '/manage/user/getApiToken', user);
  }

  resetApiToken(user: JDUser) {
    return this.http.post<string>(HttpService.getBaseURL() + '/manage/user/resetApiToken', user);
  }

}
