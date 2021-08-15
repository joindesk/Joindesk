import { Component, OnInit, Input } from '@angular/core';
import { HttpService } from '../../http.service';

@Component({
  selector: 'app-usermedia',
  templateUrl: './usermedia.component.html',
  styleUrls: ['./usermedia.component.css']
})
export class UsermediaComponent implements OnInit {
  baseURL: string;
  @Input() pic: string;
  @Input() tooltip: string;
  @Input() user: any;
  @Input() s = 'd32';
  color = "#fff";
  bgColor = 'rgb(85, 189, 40)';
  bgColors = ['rgb(85, 189, 40)', 'rgb(153, 0, 0)	', 'rgb(204, 0, 0)', 'rgb(17, 85, 204)', 'rgb(241, 194, 50)', 'rgb(230, 145, 56)', 'rgb(103, 78, 167)', 'rgb(0, 175, 224)'];
  constructor() { }

  ngOnInit() {
    this.baseURL = HttpService.getBaseURL();
    if (this.user)
      this.bgColor = this.bgColors[Math.abs(this.hashCode(this.user.userName) % 8)];
    else
      this.bgColor = this.bgColors[Math.abs(this.hashCode(this.tooltip) % 8)];
  }

  hashCode(s) {
    return s.split("").reduce(function (a, b) { a = ((a << 5) - a) + b.charCodeAt(0); return a & a }, 0);
  }

  getInitials = function (string) {
    if (string.indexOf(" (") > 0)
      string = string.substring(0, string.indexOf(" ("));
    var names = string.split(' '),
      initials = names[0].substring(0, 1).toUpperCase();

    if (names.length > 1) {
      initials += names[names.length - 1].substring(0, 1).toUpperCase();
    }
    return initials;
  }

}
