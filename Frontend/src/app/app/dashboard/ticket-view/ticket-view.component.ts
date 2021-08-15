import { IssueService } from './../../issue/issue.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DashboardService } from './../dashboard.service';
import { Component, OnInit } from '@angular/core';
import { Issue } from '../../issue/issue';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-ticket-view',
  templateUrl: './ticket-view.component.html',
  styleUrls: ['./ticket-view.component.css']
})
export class TicketViewComponent implements OnInit {
  issue: Issue;
  constructor(private dashboardService: DashboardService, private titleservice: Title,
    private route: ActivatedRoute, private router: Router, private issueService: IssueService) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.issueService.getIssue(this.dashboardService.currentProjectKey, +params['ticketID']).subscribe(resp => {
        this.issue = resp;
        this.titleservice.setTitle(this.titleservice.getTitle() + ' > [' + this.issue.keyPair + '] ' + this.issue.summary);
      });
    });
  }

}
