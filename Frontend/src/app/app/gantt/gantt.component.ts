import { Component, OnInit } from '@angular/core';
import { IssueService } from '../issue/issue.service';
import { AuthenticationService } from '../../auth/authentication.service';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { ProjectService } from '../project-manage/project.service';
import { IssueModalService } from '../issue/issue-modal.service';
import { getDaysInMonth, setDate, getISODay, getDayOfYear, startOfMonth, endOfMonth, isBefore, isEqual, getDate, isAfter } from 'date-fns'
@Component({
  selector: 'app-gantt',
  templateUrl: './gantt.component.html',
  styleUrls: ['./gantt.component.css']
})
export class GanttComponent implements OnInit {
  projectKey;
  currentuser;
  loading = false;
  currentMonth = new Date();
  events = {};
  filter = {
    start: undefined, end: undefined
  }
  days = [];
  users = {};
  issueKeys = [];
  constructor(private issueService: IssueService, private authService: AuthenticationService, private route: ActivatedRoute,
    private sanitizer: DomSanitizer, private projectService: ProjectService, private issueModalService: IssueModalService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    if (this.authService.currentUser)
      this.currentuser = this.authService.currentUser;
    this.authService.authChange.subscribe(c => {
      this.currentuser = this.authService.currentUser;
    });
    this.currentMonth = new Date();
    this.fetchEvents();
    this.projectService.getProjectMembers(this.projectKey).subscribe(resp => {
      resp.forEach(r => {
        this.users[r.userName] = r;
      })
      this.users['-unassigned-'] = { fullName: 'unassigned', pic: 'unassigned' };
    });
    this.issueService.issueNotification.subscribe(resp => {
      resp = JSON.parse(resp);
      if (this.projectKey == resp["project"])
        this.issueKeys.filter(i => i.key == resp["issue"]).forEach(i => {
          if (i.updated != resp["updated"]) {
            this.fetchEvents();
          }
        })
    })
  }

  setMonth() {
    setTimeout(() => {
      this.fetchEvents();
    }, 500);
  }

  getDaysArrayByMonth() {
    var daysInMonth = getDaysInMonth(this.currentMonth);
    var arrDays = [];
    var c = 1;

    while (daysInMonth >= c) {
      var current = this.currentMonth;
      current = setDate(current, c);
      arrDays.push({
        date: c,
        today: getDayOfYear(new Date()) == getDayOfYear(current),
        day: getISODay(current),
        d: current
      });
      c++;
    }
    return arrDays;
  }

  fetchEvents(): void {
    this.issueKeys = [];
    this.loading = true;
    const daysInMonth = getDaysInMonth(this.currentMonth);
    this.days = this.getDaysArrayByMonth();
    this.filter.start = startOfMonth(this.currentMonth);
    this.filter.end = endOfMonth(this.currentMonth);
    this.issueService.getIssuesBetween(this.projectKey, this.filter.start.toISOString().substring(0, 10), this.filter.end.toISOString().substring(0, 10))
      .subscribe(resp => {
        this.events = {};
        resp.issues.forEach(i => {
          const uid = i.assignee ? i.assignee.userName : '-unassigned-';
          if (!this.events[uid]) {
            this.events[uid] = [];
          }
          if (!i.startDate && i.endDate) i.startDate = i.endDate;
          if (i.startDate && !i.endDate) i.endDate = i.startDate;
          var s = getDate(new Date(i.startDate));
          var e = getDate(new Date(i.endDate));
          if (isBefore(new Date(i.endDate), this.filter.start)) return
          if (isEqual(new Date(i.startDate), this.filter.start) || isBefore(new Date(i.startDate), this.filter.start))
            s = 1; else s = getDate(new Date(i.startDate));

          if (isEqual(new Date(i.endDate), this.filter.end) || isAfter(new Date(i.endDate), this.filter.end))
            e = daysInMonth + 1; else e = +getDate(new Date(i.endDate)) + 1;
          this.issueKeys.push({ 'key': i.keyPair, 'updated': i.updated });
          this.events[uid].push({
            title: '[' + i.keyPair + '] ' + i.summary,
            grid_column_start: s,
            grid_column_end: e,
            grid: this.sanitizer.bypassSecurityTrustStyle(s + "/" + (e + 1)),
            id: i.keyPair,
            issueLifeCycle: i.currentStep.issueStatus.issueLifeCycle,
            user: i.assignee ? i.assignee.fullName : 'unassigned',
            start: new Date(
              i.startDate
            ),
            end: new Date(
              i.endDate
            )
          });
        });
        this.loading = false;
      });
  }

  openIssue(keyPair) {
    //this.router.navigateByUrl('/issue/' + $event.event.id);
    this.issueModalService.openIssueModal(keyPair);
  }
}
