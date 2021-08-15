import { AuthenticationService } from './../../auth/authentication.service';
import { DashboardService } from './../dashboard/dashboard.service';
import { Component, AfterViewInit, ElementRef } from '@angular/core';
import { FilteredIssues } from '../issue/issue';
import { IssueModalService } from '../issue/issue-modal.service';
declare var Chart: any;

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements AfterViewInit {
  overview = [];
  data: FilteredIssues;
  dueData: FilteredIssues;
  weekLogOverviewData = undefined;
  loading = true;
  loadingAssignedToMe = true;
  loadingDue = true;
  workData = [];
  chart;

  constructor(private dashboardService: DashboardService, public authenticationService: AuthenticationService,
    private issueModalService: IssueModalService, private elementRef: ElementRef) {

  }

  ngAfterViewInit() {
    this.getProjectsOverview();
    this.weekLogOverview();
  }

  getProjectsOverview() {
    this.dashboardService.getProjectOverview().subscribe(resp => {
      this.loading = false;
      this.overview = resp;
      if (this.overview.length > 0) {
        this.getAssignedToMe();
        this.getDue();
      }
      resp.forEach(p => {
        p.report.issueResolutionMap.forEach(r => {
          if (r.name == "OPEN")
            p['OPEN'] = r.value;
          if (r.name == "COMPLETED")
            p['COMPLETED'] = r.value;
        });
      });
    });
  }

  getAssignedToMe() {
    this.dashboardService.getAssignedToMe().subscribe(resp => {
      this.loadingAssignedToMe = false;
      this.data = resp;
    });
  }

  getDue() {
    this.dashboardService.getDue(5).subscribe(resp => {
      this.loadingDue = false;
      this.dueData = resp;
    });
  }

  isDue(date) {
    return new Date(date) < new Date();
  }

  weekLogOverview() {
    this.dashboardService.weekLogOverview().subscribe(resp => {
      this.weekLogOverviewData = JSON.parse(resp.data);
      if (resp.data != "{}") {
        this.workData.push(this.weekLogOverviewData['SUNDAY'] ? (this.weekLogOverviewData['SUNDAY'] / 60).toFixed(2) : 0);
        this.workData.push(this.weekLogOverviewData['MONDAY'] ? (this.weekLogOverviewData['MONDAY'] / 60).toFixed(2) : 0);
        this.workData.push(this.weekLogOverviewData['TUESDAY'] ? (this.weekLogOverviewData['TUESDAY'] / 60).toFixed(2) : 0);
        this.workData.push(this.weekLogOverviewData['WEDNESDAY'] ? (this.weekLogOverviewData['WEDNESDAY'] / 60).toFixed(2) : 0);
        this.workData.push(this.weekLogOverviewData['THURSDAY'] ? (this.weekLogOverviewData['THURSDAY'] / 60).toFixed(2) : 0);
        this.workData.push(this.weekLogOverviewData['FRIDAY'] ? (this.weekLogOverviewData['FRIDAY'] / 60).toFixed(2) : 0);
        this.workData.push(this.weekLogOverviewData['SATURDAY'] ? (this.weekLogOverviewData['SATURDAY'] / 60).toFixed(2) : 0);
        if (this.chart) {
          this.chart.destroy();
        }
        //console.log(this.workData);
        let htmlRef = this.elementRef.nativeElement.querySelector(`#workLogChart`);
        this.chart = new Chart(htmlRef, {
          type: 'bar',
          data: {
            labels: ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'],
            datasets: [{ data: this.workData, label: 'Hours', backgroundColor: '#2FD0A5', hoverBackgroundColor: '#2FD0A5', borderColor: '#2FD0A5' }]
          },
          options: {
            responsive: true,
            scales: { yAxes: [{ ticks: { beginAtZero: true, stepSize: 1 } }] },
            legend: {
              display: false,
              labels: {
                fontColor: '#3079ab'
              }
            }
          }
        });
      }
    });
  }

  getPercentage(p: any) {
    const a = p['OPEN'] + p['COMPLETED'];
    const b = p['OPEN'];
    if (a == 0) return 100;
    if (p['COMPLETED'] > 0 && b == 0) return 100;
    if (b == 0 && p['COMPLETED'] == 0) return 0;
    return Math.round(100 * Math.abs((a - b) / a));
  }

  openIssue(keyPair) {
    //this.router.navigateByUrl('/issue/' + $event.event.id);
    this.issueModalService.openIssueModal(keyPair);
  }

}
