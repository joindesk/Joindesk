import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Routes, RouterModule } from '@angular/router';
import { SharedModule } from './shared.module';
import { FormsModule } from '@angular/forms';
import { ProjectManageComponent } from './app/project-manage/project-manage.component';
import { DashboardHomeComponent } from './app/dashboard/dashboard-home/dashboard-home.component';
import { TicketViewComponent } from './app/dashboard/ticket-view/ticket-view.component';
import { ProjectReportComponent } from './app/project-report/project-report.component';
import { GanttComponent } from './app/gantt/gantt.component';
import { BoardOverviewComponent } from './app/dashboard/board/board-overview/board-overview.component';
import { BoardViewComponent } from './app/dashboard/board/board-view/board-view.component';
import { CalendarComponent } from './app/calendar/calendar.component';
import { ReleaseComponent } from './app/project/release/release.component';
import { IssueListComponent } from './app/dashboard/list/issue-list.component';
import { NzTreeSelectModule } from 'ng-zorro-antd/tree-select';
import { NzTreeModule } from 'ng-zorro-antd/tree';
import { NzPopoverModule } from 'ng-zorro-antd/popover';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzBreadCrumbModule } from 'ng-zorro-antd/breadcrumb';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { FullCalendarModule } from "@fullcalendar/angular";
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { PageLayoutComponent } from './app/pages/page-layout/page-layout.component';
import { PageViewComponent } from './app/pages/page-view/page-view.component';
import { PageEditComponent } from './app/pages/page-edit/page-edit.component';
import { PageHistoryComponent } from './app/pages/page-history/page-history.component';

FullCalendarModule.registerPlugins([
  dayGridPlugin,
  timeGridPlugin,
  interactionPlugin
])

const projectRoutes: Routes = [
  {
    path: "calendar",
    component: CalendarComponent,
    data: { title: "Calendar" }
  },
  {
    path: "gantt",
    component: GanttComponent,
    data: { title: "Gantt" }
  },
  {
    path: "release",
    component: ReleaseComponent,
    data: { title: "Release" }
  },
  {
    path: "board",
    component: BoardOverviewComponent,
    data: { title: "Board" },
    children: [{ path: ":boardID", component: BoardViewComponent }]
  },
  {
    path: "page",
    component: PageLayoutComponent,
    data: { title: "Page" },
    children: [
      { path: "", component: PageViewComponent },
      { path: ":wikiID", component: PageViewComponent },
      {
        path: "edit/:wikiID",
        data: { title: "Edit" },
        component: PageEditComponent
      },
      {
        path: ":wikiID/history",
        component: PageHistoryComponent
      },
    ]
  },
  {
    path: "reports",
    component: ProjectReportComponent,
    data: { title: "Reports" }
  },
  {
    path: "manage",
    component: ProjectManageComponent,
    data: { title: "Manage", animation: "Manage" },
    loadChildren: () => import('./project-manage.module').then(m => m.ProjectManageModule)
  },
  {
    path: "",
    component: DashboardHomeComponent,
    data: { title: "Issue" },
    children: [{ path: ":ticketID", component: TicketViewComponent }]
  }
];

@NgModule({
  declarations: [
    DashboardHomeComponent,
    TicketViewComponent,
    IssueListComponent,
    CalendarComponent,
    GanttComponent,
    BoardOverviewComponent,
    BoardViewComponent,
    PageLayoutComponent,
    PageViewComponent,
    PageEditComponent,
    PageHistoryComponent,
    ProjectReportComponent,
    ReleaseComponent
  ],
  exports: [
    DashboardHomeComponent,
    TicketViewComponent,
    IssueListComponent,
    CalendarComponent,
    GanttComponent,
    BoardOverviewComponent,
    BoardViewComponent,
    PageLayoutComponent,
    PageViewComponent,
    PageEditComponent,
    PageHistoryComponent,
    ProjectReportComponent,
    ReleaseComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    NzTreeSelectModule,
    NzTreeModule,
    NzPaginationModule,
    NzPopoverModule,
    NzBreadCrumbModule,
    NzDrawerModule,
    RouterModule.forChild(projectRoutes),
    FullCalendarModule
  ]
})
export class ProjectModule { }
