import { DashboardService } from './../../app/dashboard/dashboard.service';
import { MyaccountService } from './../../app/myaccount/myaccountservice.service';
import { AuthenticationService } from './../../auth/authentication.service';
import { Component, OnInit, Injectable, ViewChild, TemplateRef, ViewContainerRef, ComponentFactoryResolver } from '@angular/core';
import { NzDrawerRef } from 'ng-zorro-antd/drawer';
import { NzNotificationDataFilled, NzNotificationService } from 'ng-zorro-antd';
import { IssueService } from './../../app/issue/issue.service';
import { Router } from '@angular/router';
import { CreateComponent } from './../../app/issue/create/create.component';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  showCreateDrawer = false;
  createdNotification: NzNotificationDataFilled;
  @ViewChild('createdNotificationTemplate', { static: false }) createdNotificationTemplate: TemplateRef<{}>;
  @ViewChild('drawerTemplate', { static: false }) drawerTemplate: TemplateRef<{
    $implicit: { value: string };
    drawerRef: NzDrawerRef<string>;
  }>;
  @ViewChild('createContainer', { static: false, read: ViewContainerRef }) entry: ViewContainerRef;
  constructor(public authenticationService: AuthenticationService, public dashboardService: DashboardService, private issueService: IssueService,
    public myAccountService: MyaccountService, private notification: NzNotificationService,
    private router: Router, private resolver: ComponentFactoryResolver) {
  }

  ngOnInit() {
    this.myAccountService.get();
    this.issueService.issueCreateNotification.subscribe(i => {
      this.notification.template(this.createdNotificationTemplate, { nzDuration: 30000, nzKey: 'created-notification-' + i.keyPair, nzData: { keyPair: i.keyPair } });
    })
  }

  logout() {
    this.authenticationService.logout();
  }


  createComponent() {
    this.entry.clear();
    const factory = this.resolver.resolveComponentFactory(CreateComponent);
    const componentRef = this.entry.createComponent(factory);
    //componentRef.instance.message = message;
  }

  openComponent(): void {
    // this.drawerService.create<TicketDialogComponent, { value: string }, string>({
    //   nzTitle: 'Create',
    //   nzContent: TicketDialogComponent,
    //   nzWidth: (window.innerWidth / 3) * 2,
    //   nzContentParams: {
    //     value: this.dashboardService.currentProject.key
    //   }
    // });
    this.createComponent();
  }

  openIssue(keyPair: string) {
    this.router.navigate(['/issue', keyPair]);
  }

}
