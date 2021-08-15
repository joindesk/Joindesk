import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from '../../auth/authentication.service';
import { IssueService } from '../issue/issue.service';
import { IssueFilter } from '../issue/issue';
import { JDToastrService } from '../toastr.service';
import { ProjectReportService } from './report.service';
declare var Chart: any;

@Component({
  selector: 'app-project-report',
  templateUrl: './project-report.component.html',
  styleUrls: ['./project-report.component.css']
})
export class ProjectReportComponent implements OnInit {
  filter = {
    'type': 'rcir',
    'q': '',
    'filter': undefined,
    'group': 'daily',
    'pcrGroup': 'assignee',
    'reportFrom': undefined,
    'reportTo': undefined,
    'reportRange': undefined,
    'reportZone': undefined
  };
  pieReportData = undefined;
  crirReportData = undefined;
  rcirReportData = undefined;
  ttrReportData = undefined;
  reportFrom = undefined;
  reportTo = undefined;
  filters: IssueFilter[];
  chart;
  constructor(private route: ActivatedRoute, private reportService: ProjectReportService,
    private authService: AuthenticationService,
    private issueService: IssueService, private toastr: JDToastrService) {
    this.filter.reportZone = authService.getUserTimezone();
  }

  ngOnInit() {
    var projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    this.reportService.get(projectKey).subscribe(resp => {
      this.filter = resp;
    });
    this.issueService.getOpenFilters(projectKey).subscribe(resp => {
      this.filters = resp;
    });
  }

  generateReport() {
    if (!this.filter.filter && this.filter.type !== 'ttr') {
      this.toastr.error("Validation failed", "Choose a filter");
      return;
    }
    if (!this.filter.reportRange || this.filter.reportRange.length != 2) {
      this.toastr.error("Validation failed", "From and To dates are required");
      return;
    }
    this.filter.reportFrom = this.filter.reportRange[0].toISOString().substring(0, 10);
    this.filter.reportTo = this.filter.reportRange[1].toISOString().substring(0, 10);
    this.reportService.post(this.route.snapshot.parent.paramMap.get('projectKey'), this.filter).subscribe(resp => {
      if (this.chart) {
        this.chart.destroy();
      }
      this.pieReportData = undefined;
      this.crirReportData = undefined;
      this.rcirReportData = undefined;
      this.ttrReportData = undefined;
      this.reportFrom = resp.reportFrom;
      this.reportTo = resp.reportTo;
      if (resp.type === 'pcr') {
        this.pieReportData = JSON.parse(resp.data);
        this.chart = new Chart('myChart', {
          type: 'pie',
          data: {
            labels: this.pieReportData.labels,
            datasets: this.pieReportData.data
          },
          options: {
            responsive: true,
            legend: { position: 'left' }
          }
        });
      }
      if (resp.type === 'crir') {
        this.crirReportData = JSON.parse(resp.data);
        this.chart = new Chart('myChart', {
          type: 'line',
          data: {
            labels: this.crirReportData.labels,
            datasets: this.crirReportData.data
          },
          options: {
            responsive: true,
            scales: { yAxes: [{ ticks: { beginAtZero: true, stepSize: 1 } }] },
            legend: {
              display: true,
              labels: {
                fontColor: '#3079ab'
              }
            }
          }
        });
      }
      if (resp.type === 'rcir') {
        this.rcirReportData = JSON.parse(resp.data);
        this.chart = new Chart('myChart', {
          type: 'line',
          data: {
            labels: this.rcirReportData.labels,
            datasets: this.rcirReportData.data
          },
          options: {
            responsive: true,
            scales: { yAxes: [{ ticks: { beginAtZero: true, stepSize: 1 } }] },
            legend: {
              display: true,
              labels: {
                fontColor: '#3079ab'
              }
            }
          }
        });
      }
      if (resp.type === 'ttr') {
        this.ttrReportData = JSON.parse(resp.data);
      }
    });
  }

  formatDate(d) {
    return new Date(d).toDateString();
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }

}
