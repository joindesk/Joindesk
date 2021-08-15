import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DashboardService } from './dashboard.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  constructor(public route: ActivatedRoute, private dashboardService: DashboardService) { }

  ngOnInit() {
    this.route.paramMap.subscribe(param => {
      this.dashboardService.currentProjectKey = param.get('projectKey');
    });
  }

}
