import { Subject, Observable } from 'rxjs';
import { AuthenticationService } from './authentication.service';
import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, Router, ActivatedRoute, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';

@Injectable()
export class AdminAuthGuard implements CanActivate, CanActivateChild {

    constructor(private authenticationService: AuthenticationService, private router: Router) {
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        console.log('checking route access');
        if (this.authenticationService.verify() && this.authenticationService.isSuperAdmin()) {
            this.authenticationService.storeAuthCookies();
            return true;
        } else if (this.authenticationService.verify()) {
            this.authenticationService.storeAuthCookies();
            this.router.navigate(['/']);
        } else {
            console.log(state.url);
            this.authenticationService.redirectUrl = state.url;
            this.authenticationService.clearCookies();
            this.router.navigate(['login']);
            return false;
        }
    }

    canActivateChild() {
        console.log('checking child route access');
        return true;
    }

}
