import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterOutlet } from '@angular/router';
import { trigger, transition, query, group, style, animate, animateChild } from '@angular/animations';

@Component({
  selector: 'app-project-manage',
  templateUrl: './project-manage.component.html',
  styleUrls: ['./project-manage.component.css'],
  animations: [
    trigger('manageAnimation', [
      transition('* => *', [
        style({ position: 'relative' }),
        query(':enter, :leave', [
          style({
            position: 'absolute',
          })
        ], { optional: true }),
        query(':enter', [
          style({ opacity: 0 })
        ], { optional: true }),
        query(':leave', animateChild(), { optional: true }),
        group([
          query(':leave', [
            animate('200ms fade-out', style({ opacity: 0 }))
          ], { optional: true }),
          query(':enter', [
            animate('300ms fade-in', style({ opacity: 1 }))
          ], { optional: true })
        ]),
        query(':enter', animateChild(), { optional: true }),
      ])
    ])
  ]
})
export class ProjectManageComponent implements OnInit {
  projectKey;
  constructor(private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
  }

  prepareRoute(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData['animation'];
  }

}
