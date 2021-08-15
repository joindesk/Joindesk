import { ActivatedRoute, Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { Component, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { HTTPStatus } from './auth/token.interceptor';
import { map, filter } from 'rxjs/operators';
import { NgProgressComponent } from '@ngx-progressbar/core';
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'app';
  @ViewChild(NgProgressComponent, { static: false }) progressBar: NgProgressComponent;
  constructor(private titleService: Title, private router: Router,
    private activatedRoute: ActivatedRoute, public httpStatus: HTTPStatus) {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd))
      .pipe(map(() => this.activatedRoute))
      .pipe(map((route) => {
        let title = 'Joindesk';
        if (route.snapshot.data['title'] !== undefined && route.outlet === 'primary') {
          title = title + ' > ' + route.snapshot.data['title'];
        }
        while (route.firstChild) {
          route = route.firstChild;
          if (route.snapshot.data['title'] !== undefined && route.outlet === 'primary') {
            title = title + ' > ' + route.snapshot.data['title'];
          }
        }
        return title;
      }))
      .subscribe((event) => this.titleService.setTitle(event));
  }

  prepareRoute(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData['animation'];
  }
}
