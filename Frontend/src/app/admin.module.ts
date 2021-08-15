import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Routes, RouterModule } from '@angular/router';
import { AdminhomeComponent } from './app/admin/adminhome/adminhome.component';
import { AdminAuthGuard } from './auth/adminauth.guard.';
import { UserComponent } from './app/admin/user/user.component';
import { LinkTypeComponent } from './app/admin/link-type/link-type.component';
import { GlobalGroupComponent } from './app/admin/global-group/global-group.component';
import { ConfigComponent } from './app/admin/config/config.component';
import { ResolutionComponent } from './app/admin/resolution/resolution.component';
import { AccessComponent } from './app/admin/access/access.component';
import { WebhookComponent } from './app/admin/webhook/webhook.component';
import { ProjectComponent } from './app/admin/project/project.component';
import { ProjectViewComponent } from './app/admin/project/project-view/project-view.component';
import { SharedModule } from './shared.module';
import { FormsModule } from '@angular/forms';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';

const adminRoutes: Routes = [
  {
    path: "manage",
    component: AdminhomeComponent,
    canActivate: [AdminAuthGuard],
    data: { title: "Manage" },
    children: [
      {
        path: "user",
        component: UserComponent,
        data: { title: "Users" }
      },
      {
        path: "linktype",
        component: LinkTypeComponent,
        data: { title: "Link Types" }
      },
      {
        path: "group",
        component: GlobalGroupComponent,
        data: { title: "Groups" }
      },
      {
        path: "config",
        component: ConfigComponent,
        data: { title: "Configuration" }
      },
      {
        path: "resolution",
        component: ResolutionComponent,
        data: { title: "Resolution" }
      },
      {
        path: "access",
        component: AccessComponent,
        data: { title: "IP Filtering" }
      },
      {
        path: "webhook",
        component: WebhookComponent,
        data: { title: "Webhook" }
      },
      {
        path: "project",
        component: ProjectComponent,
        data: { title: "Project" },
        children: [
          { path: ":projectID", component: ProjectViewComponent }
        ]
      }
    ]
  }
];

@NgModule({
  declarations: [
    AdminhomeComponent,
    UserComponent,
    LinkTypeComponent,
    GlobalGroupComponent,
    ConfigComponent,
    ResolutionComponent,
    AccessComponent,
    WebhookComponent,
    ProjectComponent,
    ProjectViewComponent,
  ],
  exports: [
    AdminhomeComponent,
    UserComponent,
    LinkTypeComponent,
    GlobalGroupComponent,
    ConfigComponent,
    ResolutionComponent,
    AccessComponent,
    WebhookComponent,
    ProjectComponent,
    ProjectViewComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    NzDrawerModule,
    RouterModule.forChild(adminRoutes),
  ]
})
export class AdminModule { }
