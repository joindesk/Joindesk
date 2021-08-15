import { JDComponent } from './jdcomponent';
import { JDToastrService } from './../../toastr.service';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpService } from '../../http.service';

@Injectable()
export class JDComponentService {
  constructor(private http: HttpClient) { }

  getAll(projectKey: string) {
    return this.http.get<JDComponent[]>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/component/');
  }

  get(projectKey: string, id: number) {
    return this.http.get<JDComponent>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/component/' + id + '/');
  }

  save(projectKey: string, jdComponent: JDComponent) {
    return this.http.post<JDComponent>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/component/save/',
      jdComponent);
  }

  delete(projectKey: string, jdComponent: JDComponent) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/component/delete', jdComponent);
  }
}
