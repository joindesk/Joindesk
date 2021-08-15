import { AuthenticationService } from './authentication.service';
import { Injectable } from '@angular/core';
import {
    HttpRequest,
    HttpHandler,
    HttpEvent,
    HttpInterceptor
} from '@angular/common/http';
import { throwError, Observable, BehaviorSubject } from 'rxjs';
import { catchError, finalize, map, takeUntil } from 'rxjs/operators';

@Injectable()
export class HTTPStatus {
    private requestInFlight$: BehaviorSubject<boolean>;
    constructor() {
        this.requestInFlight$ = new BehaviorSubject(false);
    }

    setHttpStatus(inFlight: boolean) {
        this.requestInFlight$.next(inFlight);
    }

    getHttpStatus(): Observable<boolean> {
        return this.requestInFlight$.asObservable();
    }
}

@Injectable()
export class TokenInterceptor implements HttpInterceptor {

    constructor(public auth: AuthenticationService, private status: HTTPStatus) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        let deviceInfo = localStorage.getItem('JD-D');
        if (!deviceInfo || deviceInfo === null || deviceInfo === "null") {
            this.auth.getFingerPrint();
        }
        if (this.auth && this.auth.getToken()) {
            if (request.url.indexOf('attach') <= 0) {
                request = request.clone({
                    setHeaders: {
                        'X-AUTH-TOKEN': this.auth.getToken(),
                        'X-TZ': Intl.DateTimeFormat().resolvedOptions().timeZone,
                        'X-JD-D': deviceInfo,
                        'Content-Type': 'application/json'
                    }
                });
            } else {
                request = request.clone({
                    setHeaders: {
                        'X-AUTH-TOKEN': this.auth.getToken(),
                        'X-TZ': Intl.DateTimeFormat().resolvedOptions().timeZone,
                        'X-JD-D': deviceInfo
                    }
                });
            }
        } else {
            request = request.clone({
                setHeaders: {
                    'X-TZ': Intl.DateTimeFormat().resolvedOptions().timeZone,
                    'X-JD-D': deviceInfo
                }
            });
        }
        this.status.setHttpStatus(true);
        return next.handle(request)
            .pipe(takeUntil(this.auth.onCancelPendingRequests()))
            .pipe(
                map(event => {
                    return event;
                }),
                catchError(error => {
                    return throwError(error);
                }),
                finalize(() => {
                    this.status.setHttpStatus(false);
                })
            );
    }
}
