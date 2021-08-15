import { WorkflowTransition } from './workflow-transition';
import { WorkflowStep } from './workflow-step';
import { Workflow } from './workflow';
import { Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { WorkflowTransitionProperties } from './workflow-transition-properties';
import { HttpService } from '../http.service';

@Injectable()
export class WorkflowService {
  constructor(private http: HttpClient) { }
  public changeEvent = new Subject();

  getWorkflows(projectKey: string) {
    return this.http.get<Workflow[]>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/');
  }

  getWorkflowsExcept(projectKey: string, workflowID: number) {
    return this.http.get<Workflow[]>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/except/' + workflowID);
  }

  getWorkflow(projectKey: string, id: number) {
    return this.http.get<Workflow>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/' + id + '/');
  }

  createWorkflow(projectKey: string, workflow: Workflow) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/create/', workflow);
  }

  addWorkflowStep(projectKey: string, workflowID: number, step: WorkflowStep) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/' + workflowID + '/step/add/', step);
  }

  updateWorkflowStep(projectKey: string, workflowID: number, step: WorkflowStep) {
    return this.http.put<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/' + workflowID + '/step/update/', step);
  }

  removeWorkflowStep(projectKey: string, workflowID: number, stepID: number) {
    return this.http.delete<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/'
      + workflowID + '/step/remove/' + stepID);
  }

  getTransitionproperties(projectKey: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey
      + '/workflow/transition/properties');
  }

  getWorkflowTransitionproperties(projectKey: string, workflowID: number, transitionID: number) {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey
      + '/workflow/' + workflowID + '/transition/' + transitionID + '/properties');
  }

  getWorkflowTransitionPropertyFields(projectKey: string, workflowID: number, transitionID: number, subType: string) {
    return this.http.get<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey
      + '/workflow/' + workflowID + '/transition/' + transitionID + '/properties/' + subType);
  }

  addWorkflowTransition(projectKey: string, workflowID: number, t: WorkflowTransition) {
    return this.http.post<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/' + workflowID + '/transition/add/', t);
  }

  updateWorkflowTransition(projectKey: string, workflowID: number, t: WorkflowTransition) {
    return this.http.put<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/' + workflowID + '/transition/update/', t);
  }

  removeWorkflowTransition(projectKey: string, workflowID: number, t: number) {
    return this.http.delete<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/'
      + workflowID + '/transition/remove/' + t);
  }

  save(projectKey: string, workflow: Workflow) {
    return this.http.put<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/update/' + workflow.id,
      workflow);
  }

  update(projectKey: string, id: number, field: string, member: string) {
    return this.http.put<any>(HttpService.getBaseURL() + '/manage/project/' + projectKey + '/workflow/' + id +
      '/update/?field=' + field + '&value=' + member, '');
  }

  saveTransitionProperties(projectKey: string, workflowID: number, transitionID: number,
    workflowTransitionProperty: WorkflowTransitionProperties) {
    return this.http.post<WorkflowTransitionProperties>(HttpService.getBaseURL() + '/manage/project/' + projectKey +
      '/workflow/' + workflowID + '/transition/' + transitionID + '/properties/save', workflowTransitionProperty);
  }

  deleteTransitionProperties(projectKey: string, workflowID: number, transitionID: number,
    workflowTransitionProperty: WorkflowTransitionProperties) {
    return this.http.post<WorkflowTransitionProperties>(HttpService.getBaseURL() + '/manage/project/' + projectKey +
      '/workflow/' + workflowID + '/transition/' + transitionID + '/properties/remove', workflowTransitionProperty);
  }

}
