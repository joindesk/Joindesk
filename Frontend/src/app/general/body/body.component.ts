import { Component, OnInit } from '@angular/core';
import { RouterOutlet, Router, Event, NavigationStart, NavigationEnd, NavigationCancel, NavigationError } from '@angular/router';
import { slideInAnimation } from './animations';
import { DashboardService } from './../../app/dashboard/dashboard.service';
import { NzNotificationService } from 'ng-zorro-antd';

@Component({
  selector: 'app-body',
  templateUrl: './body.component.html',
  styleUrls: ['./body.component.css'],
  animations: [
    slideInAnimation
  ]
})
export class BodyComponent implements OnInit {
  loading = false;
  constructor(private router: Router, public ds: DashboardService, private notification: NzNotificationService) {
    this.router.events.subscribe((event: Event) => {
      switch (true) {
        case event instanceof NavigationStart: {
          this.loading = true;
          break;
        }
        case event instanceof NavigationEnd:
        case event instanceof NavigationCancel:
        case event instanceof NavigationError: {
          this.loading = false;
          if (event['url'] && event['url'].indexOf("issue/") < 0)
            this.notification.remove();
          break;
        }
        default: {
          break;
        }
      }
    });
  }

  ngOnInit() {
  }

  prepareRoute(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData['animation'];
  }
}
