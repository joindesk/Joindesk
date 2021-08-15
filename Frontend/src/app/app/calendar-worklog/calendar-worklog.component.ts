import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from './../../auth/authentication.service';
import { IssueModalService } from '../issue/issue-modal.service';
import { MyaccountService } from '../myaccount/myaccountservice.service';
import { WorkLog } from '../issue/issue';
import { CalendarOptions } from '@fullcalendar/core';
@Component({
  selector: 'app-calendar',
  templateUrl: './calendar-worklog.component.html',
  styleUrls: ['./calendar-worklog.component.css']
})
export class CalendarWorklogComponent implements OnInit {
  currentuser;
  filter = {
    start: undefined, end: undefined
  }
  loading = false;
  events = [];
  logs: WorkLog[] = [];
  calendarOptions: CalendarOptions = {
    headerToolbar: {
      right: 'prev,next today',
      left: 'title'
    },
    initialView: 'dayGridMonth',
    initialEvents: [],
    weekends: true,
    editable: false,
    selectable: true,
    selectMirror: true,
    dayMaxEvents: true,
    datesSet: this.render.bind(this),
    select: this.handleDateClick.bind(this),
    eventClick: this.eventClicked.bind(this),
    events: this.render.bind(this)
  };
  constructor(private authService: AuthenticationService, private myaccountService: MyaccountService,
    private issueModalService: IssueModalService) { }

  ngOnInit() {
    this.authService.authChange.subscribe(c => {
      this.currentuser = this.authService.currentUser;
    });
    this.issueModalService.worklogChanged.subscribe(r => this.fetchEvents(this.filter.start, this.filter.end));
  }

  fetchEvents(start, end) {
    this.myaccountService.getWorklogReport(start, end).subscribe(resp => {
      this.events = [];
      this.logs = resp;
      resp.forEach(i => {
        const title = (i.workDescription) ? i.workDescription : '';
        this.events.push({
          title: '[' + i.issue.keyPair + '] ' + title,
          id: i.id,
          start: new Date(
            i.from
          ),
          end: new Date(
            i.to
          ),
          backgroundColor: '#2ecaac',
          textColor: '#fff',
          meta: {
            i
          }
        });
      });
      this.loading = false;
      this.calendarOptions.events = this.events;
    })
  }

  getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  render(info, successCallback, failureCallback) {
    this.filter.start = info.startStr.substring(0, 10);
    this.filter.end = info.endStr.substring(0, 10);
    this.fetchEvents(this.filter.start, this.filter.end);
  }


  eventClicked($event) {
    this.logs.forEach(l => {
      if (l.id == $event.event.id)
        this.issueModalService.openIssueWorkLogModal(l);
    });
  }

  handleDateClick($event) {
    let worklog = new WorkLog();
    worklog.workFrom = $event.startStr;
    this.issueModalService.openIssueWorkLogModal(worklog);
  }

}
