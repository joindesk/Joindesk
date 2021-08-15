import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { HttpService } from "../http.service";

@Injectable()
export class ProjectReportService {
    constructor(private http: HttpClient) { }

    get(projectKey: string) {
        return this.http.get<any>(HttpService.getBaseURL() + '/report/' +
            projectKey + '/');
    }

    post(projectKey: string, filter: any) {
        return this.http.post<any>(HttpService.getBaseURL() + '/report/' +
            projectKey + '/', filter);
    }
}