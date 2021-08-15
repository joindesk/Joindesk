import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-icon',
  templateUrl: './icon.component.html'
})
export class IconComponent implements OnInit {
  @Input() icon: string;
  @Input() tooltip: string;
  @Input() height = '32';
  @Input() width = '32';
  @Input() fill = 'currentColor';
  constructor() { }

  ngOnInit() {
    this.icon = "/assets/img/bootstrap-icons.svg#" + this.icon
  }

}
