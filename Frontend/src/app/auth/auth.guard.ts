import { AuthenticationService } from './authentication.service';
import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, Router, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { IssueModalService } from '../app/issue/issue-modal.service';

@Injectable()
export class AuthGuard implements CanActivate, CanActivateChild {

    constructor(private authenticationService: AuthenticationService, private router: Router,
        private ims: IssueModalService) { }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        this.ims.closeAll();
        if (this.authenticationService.verify()) {
            this.authenticationService.storeAuthCookies();
            return true;
        } else {
            this.authenticationService.redirectUrl = state.url;
            this.authenticationService.logout();
            this.router.navigate(['login']);
            return false;
        }
    }

    canActivateChild() {
        return true;
    }

}
