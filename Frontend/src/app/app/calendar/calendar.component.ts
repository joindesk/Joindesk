import { Component, OnInit } from '@angular/core';
import { IssueService } from '../issue/issue.service';
import { ActivatedRoute } from '@angular/router';
import { AuthenticationService } from './../../auth/authentication.service';
import { CalendarOptions, DateSelectArg } from '@fullcalendar/angular';
import { IssueModalService } from '../issue/issue-modal.service';
import { addDays, toDate, subDays, format } from 'date-fns'
import { JDToastrService } from '../toastr.service';
@Component({
  selector: 'app-calendar',
  templateUrl: './calendar.component.html',
  styleUrls: ['./calendar.component.css']
})
export class CalendarComponent implements OnInit {
  projectKey;
  currentuser;
  filter = {
    start: undefined, end: undefined
  }
  loading = false;
  issueKeys = [];
  events = [];
  calendarOptions: CalendarOptions = {
    headerToolbar: {
      right: 'prev,next today',
      left: 'title'
    },
    initialView: 'dayGridMonth',
    initialEvents: [],
    weekends: true,
    editable: true,
    selectable: true,
    selectMirror: true,
    dayMaxEvents: true,
    datesSet: this.render.bind(this),
    select: this.handleDateSelect.bind(this),
    eventClick: this.eventClicked.bind(this),
    events: this.render.bind(this),
    eventDrop: this.eventDropped.bind(this),
    eventResize: this.eventResize.bind(this)
  };
  constructor(private issueService: IssueService, private authService: AuthenticationService, private route: ActivatedRoute,
    private issueModalService: IssueModalService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    if (this.authService.currentUser)
      this.currentuser = this.authService.currentUser;
    this.authService.authChange.subscribe(c => {
      this.currentuser = this.authService.currentUser;
    });
    console.log(this.currentuser);
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

  handleDateSelect(selectInfo: DateSelectArg) {
    console.log(selectInfo)
  }

  fetchEvents(): void {
    this.loading = true;
    this.issueService.getIssuesBetween(this.projectKey, this.filter.start, this.filter.end).subscribe(resp => {
      this.issueKeys = [];
      this.events = [];
      resp.issues.forEach(i => {
        this.issueKeys.push({ 'key': i.keyPair, 'updated': i.updated });
        let customClass = 'other';
        if (i.assignee && i.assignee.userName && i.assignee.userName === this.currentuser) {
          customClass = 'own';
        }
        if (!i.startDate && i.endDate) i.startDate = i.endDate;
        if (i.startDate && !i.endDate) i.endDate = i.startDate;
        var s = toDate(new Date(i.startDate));
        var e = toDate(new Date(i.endDate));
        e = addDays(new Date(i.endDate), 1);
        this.events.push({
          title: '[' + i.keyPair + '] ' + i.summary,
          id: i.keyPair,
          start: toDate(s),
          end: toDate(e),
          allDay: true,
          cssClass: customClass,
          eventTextColor: '#000',
          classNames: ["issueStatusCalendar", "calendarStatus-" + i.currentStep.issueStatus.issueLifeCycle],
          meta: {
            i
          }
        });
      });
      this.loading = false;
      this.calendarOptions.events = this.events;
    });
  }

  render(info, successCallback, failureCallback) {
    this.filter.start = info.startStr.substring(0, 10);
    this.filter.end = info.endStr.substring(0, 10);
    this.fetchEvents();
  }

  eventDropped(info) {
    this.changeDates(info);
  }

  eventResize(info) {
    this.changeDates(info);
  }

  changeDates(info) {
    console.log(info.event.startStr + " - " + info.event.endStr);
    console.log(subDays(new Date(info.event.endStr), 1));
    var e = format(subDays(new Date(info.event.endStr), 1), 'yyyy-MM-dd'); //info.event.endStr;
    this.issueService.quickUpdate({ field: 'datepair', data: info.event.startStr + ':' + e, issuesKeyPairs: [info.event.id] })
      .subscribe(resp => {
        this.toastr.infoMessage("Updated");
      }, err => { this.toastr.errorMessage("Error updating " + err); })
  }


  eventClicked($event) {
    //this.router.navigateByUrl('/issue/' + $event.event.id);
    this.issueModalService.openIssueModal($event.event.id);
  }

}
