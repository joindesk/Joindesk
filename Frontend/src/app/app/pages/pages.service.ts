import {
  Page, PageAttachment, PageFileProgress, PageRevision, PageData
} from "./page";
import { Subject } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { HttpService } from "../http.service";

@Injectable()
export class PageService {
  wikiChange: Subject<boolean> = new Subject<boolean>();
  public attachmentNotification = new Subject<PageFileProgress>();
  constructor(private http: HttpClient) { }

  getWikis(projectKey: string, wikiKey: string) {
    return this.http.get<Page[]>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/?wikiKey=" + wikiKey
    );
  }

  getRecent(projectKey: string) {
    return this.http.get<Page[]>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/recent/"
    );
  }

  getWiki(projectKey: string, wikiKey: number, mode: string) {
    return this.http.get<any>(
      HttpService.getBaseURL() +
      "/page/" +
      projectKey +
      "/" +
      wikiKey +
      "?mode=" +
      mode
    );
  }

  getChildrens(projectKey: string, wikiKey: string) {
    return this.http.get<any>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wikiKey + "/children"
    );
  }

  searchWiki(projectKey: string, q: string) {
    return this.http.get<Page[]>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/search?q=" + q
    );
  }

  searchFolder(projectKey: string, q: string) {
    return this.http.get<Page[]>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/searchFolder?q=" + q
    );
  }

  copyWiki(projectKey: string, wiki: Page, folder: Page) {
    return this.http.post<any>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wiki.id + "/copy",
      folder
    );
  }

  moveWiki(projectKey: string, wiki: Page, folder: Page) {
    return this.http.post<any>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wiki.id + "/move",
      folder
    );
  }

  saveWiki(projectKey: string, wiki: Page) {
    return this.http.post<any>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wiki.id,
      wiki
    );
  }

  removeWiki(projectKey: string, wiki: Page) {
    return this.http.delete<any>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wiki.id
    );
  }


  getAttachments(projectKey: string, id: number) {
    return this.http.get<PageAttachment[]>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + id + "/attachments/"
    );
  }

  public getAttachment(
    projectKey: string,
    wikiID: number,
    attachmentID: number
  ) {
    return this.http.get(
      HttpService.getBaseURL() +
      "/page/" +
      projectKey +
      "/" +
      wikiID +
      "/attachment/" +
      attachmentID,
      { responseType: "blob", observe: 'response' }
    );
  }

  public attach(projectKey: string, wikiID: number, files) {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      // create a new multipart-form for every file
      const formData: FormData = new FormData();
      formData.append("file", file, file.name);

      const status = new PageFileProgress();
      // Save every progress-observable in a map of all observables
      status.file = file.name;
      status.pageKey = wikiID;

      status.status = "Started";
      this.attachmentNotification.next(status);

      // send the http-request and subscribe for progress-updates
      this.upload(projectKey, wikiID, formData).subscribe(resp => {
        status.status = "Done";
        status.attachment = resp;
        this.attachmentNotification.next(status);
      });
    }
  }

  upload(projectKey: string, wikiID: number, formData: FormData) {
    return this.http.post<any>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wikiID + "/attach",
      formData
    );
  }

  deleteAttachment(projectKey: string, wikiID: number, a: PageAttachment) {
    return this.http.post<PageAttachment>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + wikiID + "/detach",
      a
    );
  }

  getRevisions(projectKey: string, id: number) {
    return this.http.get<PageRevision[]>(
      HttpService.getBaseURL() + "/page/" + projectKey + "/" + id + "/revisions/"
    );
  }

  compareRevisions(
    projectKey: string,
    id: number,
    source: number,
    target: number
  ) {
    return this.http.get<string>(
      HttpService.getBaseURL() +
      "/page/" +
      projectKey +
      "/" +
      id +
      "/compare/" +
      source +
      "/" +
      target
    );
  }

  getRevision(
    projectKey: string,
    id: number,
    revision: number
  ) {
    return this.http.get<PageRevision>(
      HttpService.getBaseURL() +
      "/page/" +
      projectKey + "/" + id + "/revisions/" + revision
    );
  }
}
