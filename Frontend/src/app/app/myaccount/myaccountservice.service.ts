import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { JDUser, Token } from '../admin/user/user';
import { map } from 'rxjs/operators';
import { HttpService } from '../http.service';

@Injectable()
export class MyaccountService {
  user: JDUser;
  sessions: Token[];
  constructor(private http: HttpClient) {
  }

  get() {
    return this.http.get<JDUser>(HttpService.getBaseURL() + '/my/').subscribe(resp => {
      this.user = resp;
    });
  }

  update(user: JDUser) {
    return this.http.post<JDUser>(HttpService.getBaseURL() + '/my/update', user).pipe(map(resp => {
      this.get();
    }));
  }

  changePassword(old: string, pwd: string, c_pwd: string) {
    return this.http.post<any>(HttpService.getBaseURL() + '/my/change_password',
      { 'old_password': old, 'password': pwd, 'confirm_password': c_pwd }
    );
  }

  updateNotificationPref(ele, checked) {
    if (ele === 'email') {
      this.user.emailNotification = checked;
    }
    if (ele === 'slack') {
      this.user.slackNotification = checked;
    }
    return this.update(this.user);
  }

  generatePic() {
    return this.http.get<JDUser>(HttpService.getBaseURL() + '/my/refreshPic');
  }

  removeAvatar() {
    return this.http.get<JDUser>(HttpService.getBaseURL() + '/my/removePic');
  }

  upload(formData: FormData) {
    return this.http.post<any>(HttpService.getBaseURL() + '/my/attach',
      formData).pipe(map(resp => {
        this.get();
      }));
  }

  public updatePic(files) {
    const file = files[0];
    // create a new multipart-form for every file
    const formData: FormData = new FormData();
    if (file) {
      formData.append('file', file, file.name);

      // send the http-request and subscribe for progress-updates
      return this.upload(formData).pipe(map(resp => {
        return resp;
      }));
    }
  }

  getSessions() {
    return this.http.get<Token[]>(HttpService.getBaseURL() + '/my/active').subscribe(resp => {
      this.sessions = resp;
    });
  }

  removeSession(t: Token) {
    return this.http.post<any>(HttpService.getBaseURL() + '/my/sign_out_session', t).subscribe(resp => {
      this.getSessions();
    });
  }

  removeSessions() {
    return this.http.post<any>(HttpService.getBaseURL() + '/my/sign_out_sessions', {}).subscribe(resp => {
      this.getSessions();
    });
  }

  slackConnectInit(username: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/slackConnectInit?username=' + username);
  }

  slackConnect(username: string, token: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/slackConnect?username=' + username + '&token=' + token);
  }

  slackDisconnect() {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/slackDisconnect');
  }

  activateMfa(mfaCode: number) {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/activateMFA?code=' + mfaCode);
  }

  deactivateMfa() {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/deactivateMFA');
  }

  setPreferredAuth(type: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/setPreferredAuth?type=' + type);
  }

  getWorklogReport(start: string, end: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/my/workLogReport?start=' + start + '&end=' + end);
  }

}
