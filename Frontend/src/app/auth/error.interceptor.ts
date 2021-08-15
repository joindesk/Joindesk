import { JDToastrService } from './../app/toastr.service';
import { Router, ActivatedRoute, RouterStateSnapshot } from '@angular/router';
import { AuthenticationService } from './authentication.service';
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Subject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

    constructor(private authenticationService: AuthenticationService, private router: Router, private toastr: JDToastrService) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(req).pipe(tap(event => {
            this.authenticationService.authError = false;
            this.authenticationService.connectivityError = false;
        }, err => {
            if (err instanceof HttpErrorResponse && err.status === 401) {
                // handle 401 errors
                //this.toastr.error('Connection Failure', 'Authentication Failure');
                this.authenticationService.authError = true;
                this.authenticationService.logoutSubject.next(new Date().getTime());
                // this.route.snapshot.
                this.authenticationService.redirectUrl = this.router.url;
                this.authenticationService.logout();
            } else if (err instanceof HttpErrorResponse && err.status === 403) {
                // If 403 for My account, then incorrect authentication - force
                if (err.error.path === '/my/') {
                    console.error('received 403. redirecting to login');
                    this.authenticationService.logout();
                }
            } else if (err instanceof HttpErrorResponse && err.status === 404) {
                // If 404 for Invalid TRequest
                if (err.error.message == "Invalid Tenant") {
                    //this.authenticationService.logout();
                    this.router.navigateByUrl("/login?error=InvalidTenant");
                }
            } else if (err instanceof HttpErrorResponse && (err.status === 0 || err.status === 502 || err.status === 503 || err.status === 504)) {
                this.authenticationService.connectivityError = true;
                // this.router.navigate(['login']);
            } else if (err.error && err.error.error) {
                if (err.error.errors && err.error.errors.length > 0) {
                    this.toastr.error('Error', err.error.errors);
                } else {
                    this.toastr.error('Error', err.error.error);
                }

            }
        }));
    }
}
