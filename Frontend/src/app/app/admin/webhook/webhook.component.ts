import { Subject } from 'rxjs';
import { WebHook } from './../../project-manage/webhook';
import { JDToastrService } from './../../toastr.service';
import { ManageService } from './../manage.service';
import { Component, OnInit } from '@angular/core';
import { ProjectService } from '../../project-manage/project.service';
import { Project } from '../../project-manage/project';
import { WebHookHeaders } from '../../project-manage/webhook-headers';

@Component({
  selector: 'app-webhook',
  templateUrl: './webhook.component.html',
  styleUrls: ['./webhook.component.css']
})
export class WebhookComponent implements OnInit {
  public hooks: WebHook[];
  public eventTypes: string[];
  public hook: WebHook;
  public projects: Project[];
  constructor(private manageService: ManageService, private toastr: JDToastrService,
    private projectService: ProjectService) { }

  ngOnInit() {
    this.get();
  }

  get() {
    this.manageService.getWebHooks().subscribe(resp => this.hooks = resp);
    this.manageService.getEventTypes().subscribe(resp => this.eventTypes = resp);
    this.projectService.getProjectsObservable().subscribe(resp => this.projects = resp);
  }

  create() {
    this.hook = new WebHook();
  }

  save(a, ap, ae) {
    this.hook.active = a.checked;
    this.hook.allProjects = ap.checked;
    this.hook.allEvents = ae.checked;
    this.manageService.saveWebHook(this.hook).subscribe(resp => {
      this.toastr.success('Hook', ' saved');
      this.get();
      this.cancel();
    });
  }

  open(r) {
    this.hook = r;
    this.hook.edit = true;
  }

  cancel() {
    this.hook = undefined;
  }

  delete(r: WebHook) {
    this.manageService.removeWebHook(r).subscribe(resp => {
      this.toastr.success('Hook', ' removed');
      this.get();
      this.cancel();
    });
  }

  addHeader() {
    this.hook.requestHeaders.push(new WebHookHeaders());
  }

  removeHeader(index) {
    this.hook.requestHeaders.splice(index, 1);
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }

}
