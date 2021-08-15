registerLocaleData(en); import { CookieService } from "ngx-cookie-service";
import { JDComponentService } from "./app/project-manage/jdcomponent/jdcomponent.service";
import { CustomFieldService } from "./app/project-manage/custom-field/custom-field.service";
import { BoardService } from "./app/dashboard/board/board.service";
import { ManageService } from "./app/admin/manage.service";
import { ProjectService } from "./app/project-manage/project.service";
import { MyaccountService } from "./app/myaccount/myaccountservice.service";
import { MatDialog } from "@angular/material/dialog";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { DashboardService } from "./app/dashboard/dashboard.service";
import { WorkflowService } from "./app/project-manage/workflow.service";
import { JDToastrService } from "./app/toastr.service";
import { TokenInterceptor, HTTPStatus } from "./auth/token.interceptor";
import { AuthGuard } from "./auth/auth.guard";
import { AuthenticationService } from "./auth/authentication.service";
import { LoginComponent } from "./auth/login/login.component";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { DeviceDetectorModule } from "ngx-device-detector";
import { AppComponent } from "./app.component";
import { HTTP_INTERCEPTORS } from "@angular/common/http";
import { HomeComponent } from "./app/home/home.component";
import { HeaderComponent } from "./general/header/header.component";
import { LayoutComponent } from "./general/layout/layout.component";
import { FooterComponent } from "./general/footer/footer.component";
import { BodyComponent } from "./general/body/body.component";
import { SidebarComponent } from "./general/sidebar/sidebar.component";
import { ErrorInterceptor } from "./auth/error.interceptor";
import { UserService } from "./app/admin/user/user.service";
import { GroupService } from "./app/project-manage/group.service";
import { IssueTypeService } from "./app/project-manage/issue-type.service";
import { DashboardComponent } from "./app/dashboard/dashboard.component";
import { TicketDialogComponent } from "./app/issue/ticket-dialog/ticket-dialog.component";
import { IssueService } from "./app/issue/issue.service";
import {
  IssueViewComponent,
  AppIssueViewImagePreviewComponent,
} from "./app/issue/issue-view/issue-view.component";
import { InlineEditComponent } from "./app/issue/inline-edit/inline-edit.component";
import { MyaccountComponent } from "./app/myaccount/myaccount.component";
import { AdminAuthGuard } from "./auth/adminauth.guard.";
import { TransitionEditComponent } from "./app/project-manage/project-manage-workflow/transition-edit/transition-edit.component";
import { WorklogDialogComponent } from "./app/worklog-dialog/worklog-dialog.component";
import { CustomfieldInlineComponent } from "./app/issue/customfield-inline/customfield-inline.component";
import { IssueTransitionDialogComponent } from './app/issue/issue-transition-dialog/issue-transition-dialog.component';
import { RegisterComponent } from './auth/register/register.component';
import { environment } from '../environments/environment';
import { NZ_I18N, en_US } from 'ng-zorro-antd';
import { registerLocaleData } from '@angular/common';
import en from '@angular/common/locales/en';
import { InjectionService } from "@swimlane/ngx-charts/release/common/tooltip/injection.service";
import { SharedModule } from "./shared.module";
import { WorkflowPreviewComponent } from "./app/project-manage/project-manage-workflow/workflow-preview/workflow-preview.component";
import { NgProgressModule } from '@ngx-progressbar/core';
import { NgProgressHttpModule } from '@ngx-progressbar/http';
import * as $ from 'jquery';
import { IssueModalService } from "./app/issue/issue-modal.service";
import { CalendarWorklogComponent } from "./app/calendar-worklog/calendar-worklog.component";
import { WorklogNzDialogComponent } from "./app/issue/worklog-dialog/worklog-dialog.component";
import { LaneEditComponent } from "./app/dashboard/board/lane-edit/lane-edit.component";
import { ProjectReportService } from "./app/project-report/report.service";
import { CreateComponent } from "./app/issue/create/create.component";
import { JdEditorComponent } from "./app/issue/jd-editor/jd-editor.component";
import { JwtHelperService } from '@auth0/angular-jwt';
import { SetupComponent } from "./auth/setup/setup.component";
import { ForgotComponent } from "./auth/forgot/forgot.component";
import { PageService } from "./app/pages/pages.service";

const appRoutes: Routes = [
  {
    path: "login",
    component: LoginComponent
  },
  {
    path: "register",
    component: RegisterComponent
  },
  {
    path: "forgot",
    component: ForgotComponent
  },
  {
    path: "setup",
    component: SetupComponent
  },
  {
    path: "",
    redirectTo: "/home",
    pathMatch: "full"
  },
  {
    path: "",
    component: LayoutComponent,
    children: [
      {
        path: "home",
        component: HomeComponent,
        canActivate: [AuthGuard],
        data: { title: "Home" }
      },
      {
        path: "project/:projectKey",
        component: DashboardComponent,
        canActivate: [AuthGuard],
        data: { title: "Project" },
        loadChildren: () => import('./project.module').then(m => m.ProjectModule)
      },
      {
        path: "issue/:issueKeyPair",
        component: IssueViewComponent,
        canActivate: [AuthGuard],
        data: { title: "Issue" }
      },
      {
        path: "myaccount",
        component: MyaccountComponent,
        canActivate: [AuthGuard],
        data: { title: "My Account" }
      },
      // {
      //   path: "worklog",
      //   component: CalendarWorklogComponent,
      //   data: { title: "Worklog Calendar" }
      // },
      {
        path: "admin",
        component: DashboardComponent,
        canActivate: [AuthGuard],
        data: { title: "Administration" },
        //loadChildren: './admin.module#AdminModule'
        loadChildren: () => import('./admin.module').then(m => m.AdminModule)
      }
    ]
  },
  { path: "**", redirectTo: "/home" }
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    HeaderComponent,
    LayoutComponent,
    FooterComponent,
    BodyComponent,
    SidebarComponent,
    DashboardComponent,
    IssueViewComponent,
    TicketDialogComponent,
    CalendarWorklogComponent,
    MyaccountComponent,
    WorklogDialogComponent,
    WorklogNzDialogComponent,
    IssueTransitionDialogComponent,
    RegisterComponent,
    ForgotComponent,
    SetupComponent,
    TransitionEditComponent,
    AppIssueViewImagePreviewComponent,
    LaneEditComponent,
    InlineEditComponent,
    CustomfieldInlineComponent,
    TicketDialogComponent,
    CreateComponent,
    JdEditorComponent
  ],
  imports: [
    BrowserModule,
    SharedModule,
    FormsModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    DeviceDetectorModule.forRoot(),
    RouterModule.forRoot(appRoutes),
    NgProgressModule,
    NgProgressHttpModule
  ],
  exports: [],
  providers: [
    AuthenticationService,
    AuthGuard,
    AdminAuthGuard,
    JDToastrService,
    ProjectService,
    GroupService,
    IssueTypeService,
    WorkflowService,
    UserService,
    DashboardService,
    IssueService,
    IssueModalService,
    ManageService,
    MyaccountService,
    JDComponentService,
    BoardService,
    InjectionService,
    ProjectReportService,
    PageService,
    CustomFieldService,
    CookieService,
    JwtHelperService,
    MatDialog,
    HTTPStatus,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    { provide: NZ_I18N, useValue: en_US }
  ],
  bootstrap: [AppComponent],
  entryComponents: [
    TicketDialogComponent,
    AppIssueViewImagePreviewComponent,
    IssueTransitionDialogComponent,
    WorklogDialogComponent,
    WorklogNzDialogComponent,
    LaneEditComponent,
    TransitionEditComponent,
    WorkflowPreviewComponent,
    CreateComponent
  ]
})
export class AppModule { }
