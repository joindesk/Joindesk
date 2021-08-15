import { DashboardService } from './../../app/dashboard/dashboard.service';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService } from './../../auth/authentication.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  expandProject: Boolean;

  constructor(public authenticationService: AuthenticationService,
    private router: Router, public dashboardService: DashboardService) { }

  ngOnInit() { }

  generateIcon(name: string) {
    let icon = '';
    const sp = name.split(' ');
    if (sp.length > 1) {
      sp.forEach(el => {
        if (icon.length <= 2) {
          icon = icon + el.substr(0, 1);
        }
      });
    } else {
      icon = sp[0].substr(0, 2);
    }
    return icon;
  }

  changingCurrentProject() {
    this.router.navigate(['/project', this.dashboardService.currentProject.key]);
  }
}
