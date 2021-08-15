import { JDToastrService } from './../../toastr.service';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CustomField } from './custom-field';
import { HttpService } from '../../http.service';

@Injectable()
export class CustomFieldService {
  constructor(private http: HttpClient, private toastr: JDToastrService) { }

  getAll(projectKey: string) {
    return this.http.get<CustomField[]>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/custom-field/');
  }

  get(projectKey: string, id: number) {
    return this.http.get<CustomField>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/custom-field/' + id + '/');
  }

  save(projectKey: string, customField: CustomField) {
    return this.http.post<CustomField>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/custom-field/save/',
      customField);
  }

  delete(projectKey: string, customField: CustomField) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/custom-field/delete', customField);
  }
}
