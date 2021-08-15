import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Routes, RouterModule } from '@angular/router';
import { SharedModule } from './shared.module';
import { FormsModule } from '@angular/forms';
import { ProjectManageOverviewComponent } from './app/project-manage/project-manage-overview/project-manage-overview.component';
import { ProjectManageDetailComponent } from './app/project-manage/project-manage-detail/project-manage-detail.component';
import { ProjectManageIssueTypesComponent } from './app/project-manage/project-manage-issue-types/project-manage-issue-types.component';
import { ProjectManageVersionComponent } from './app/project-manage/project-manage-version/project-manage-version.component';
import { ProjectManageTimetrackingComponent } from './app/project-manage/project-manage-timetracking/project-manage-timetracking.component';
import { CustomFieldsComponent } from './app/project-manage/custom-field/custom-fields.component';
import { JDCntrlrComponent } from './app/project-manage/jdcomponent/jdcomponent.component';
import { ProjectManageWorkflowComponent } from './app/project-manage/project-manage-workflow/project-manage-workflow.component';
import { WorkflowViewComponent } from './app/project-manage/project-manage-workflow/workflow-view/workflow-view.component';
import { WorkflowEditComponent } from './app/project-manage/project-manage-workflow/workflow-edit/workflow-edit.component';
import { ProjectManageGroupComponent } from './app/project-manage/project-manage-group/project-manage-group.component';
import { ProjectManageRepositoryComponent } from './app/project-manage/project-manage-repository/project-manage-repository.component';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';

const adminRoutes: Routes = [
  { path: "", component: ProjectManageOverviewComponent, data: { animation: '1' } },
  { path: "detail", component: ProjectManageDetailComponent, data: { animation: '2' } },
  {
    path: "issue-type",
    component: ProjectManageIssueTypesComponent, data: { animation: '3' }
  },
  { path: "version", component: ProjectManageVersionComponent },
  { path: "repository", component: ProjectManageRepositoryComponent },
  {
    path: "timetracking",
    component: ProjectManageTimetrackingComponent
  },
  { path: "custom-field", component: CustomFieldsComponent },
  { path: "component", component: JDCntrlrComponent },
  {
    path: "workflow",
    component: ProjectManageWorkflowComponent,
    children: [
      { path: ":workflowID", component: WorkflowViewComponent },
      {
        path: "edit/:workflowID",
        component: WorkflowEditComponent
      }
    ]
  },
  { path: "group", component: ProjectManageGroupComponent }
];

@NgModule({
  declarations: [
    ProjectManageOverviewComponent,
    ProjectManageDetailComponent,
    ProjectManageIssueTypesComponent,
    ProjectManageVersionComponent,
    ProjectManageRepositoryComponent,
    ProjectManageTimetrackingComponent,
    CustomFieldsComponent,
    JDCntrlrComponent,
    ProjectManageWorkflowComponent,
    WorkflowViewComponent,
    WorkflowEditComponent,
    ProjectManageGroupComponent
  ],
  exports: [
    ProjectManageOverviewComponent,
    ProjectManageDetailComponent,
    ProjectManageIssueTypesComponent,
    ProjectManageVersionComponent,
    ProjectManageRepositoryComponent,
    ProjectManageTimetrackingComponent,
    CustomFieldsComponent,
    JDCntrlrComponent,
    ProjectManageWorkflowComponent,
    WorkflowViewComponent,
    WorkflowEditComponent,
    ProjectManageGroupComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    NzDrawerModule,
    RouterModule.forChild(adminRoutes),
  ]
})
export class ProjectManageModule { }
