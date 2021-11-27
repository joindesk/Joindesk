import { map, catchError } from 'rxjs/operators';
import { Project } from './../app/project-manage/project';
import { JDUser } from './../app/admin/user/user';
import { Router } from '@angular/router';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DeviceDetectorService } from 'ngx-device-detector';
import { CookieService } from 'ngx-cookie-service';
import { Subject } from 'rxjs';
import { MyaccountService } from '../app/myaccount/myaccountservice.service';
import { HttpService } from '../app/http.service';
import { JwtHelperService } from '@auth0/angular-jwt';

@Injectable()
export class AuthenticationService {
    private cancelPendingRequests$ = new Subject<void>()
    public connectivityError = false;
    public authError = false;
    logoutSubject = new Subject<number>();
    redirectUrl = '/home';
    public loading = false;
    public projects: Project[];
    public currentUser: JDUser;
    public decodedToken: any;
    public authChange = new Subject<number>();
    public canCreate = false;
    constructor(private http: HttpClient, private router: Router, private deviceService: DeviceDetectorService,
        private cookieService: CookieService, public myAccountService: MyaccountService) {
        this.logoutSubject.subscribe(lg => {
            this.authError = true;
        });
    }

    public cancelPendingRequests() {
        this.cancelPendingRequests$.next()
    }

    public onCancelPendingRequests() {
        return this.cancelPendingRequests$.asObservable()
    }

    checkUser(username: string, mode: string) {
        const l: any = {};
        l.username = username;
        l.mode = mode;
        return this.http.post<any>(HttpService.getBaseURL() + '/auth/checkUser', l);
    }

    getFingerPrint() {
        var deviceInfo = this.deviceService.getDeviceInfo();
        localStorage.setItem('JD-D', btoa(deviceInfo.browser + "-" + deviceInfo.device + "-" + deviceInfo.os + "-" + deviceInfo.os_version));
    }

    login(username: string, password: string, mode: string) {
        let deviceInfo = localStorage.getItem('JD-D');
        if (!deviceInfo || deviceInfo === null || deviceInfo === "null") {
            this.getFingerPrint();
        }
        const l: any = {};
        l.username = username;
        l.password = password;
        //l.mode = mode;
        l.remember = mode;
        l.deviceFp = localStorage.getItem('JD-D');
        l.deviceInfo = btoa(JSON.stringify(this.deviceService.getDeviceInfo()));
        return this.http.post<any>(HttpService.getBaseURL() + '/auth/login', l,
            { observe: 'response' })
            .pipe(map(resp => {
                if (resp.status === 200 && resp.body.success) {
                    const token = resp.headers.get('X-AUTH-TOKEN');
                    localStorage.setItem('JD-T', token);
                    this.getUser();
                    this.storeAuthCookies();
                    return resp.body;
                } else {
                    localStorage.removeItem('JD-T');
                }
                return resp.body;
            })).pipe(catchError((e) => {
                console.log(e);
                return e;
            }));
    }

    storeAuthCookies() {
        const token = localStorage.getItem('JD-T');
        if (token) {
            this.cookieService.set('jdt', token, 1, '/', null, true, "None");
            //this.cookieService.set('jdt', token.substr(0, token.length / 2), 365, '/');
            //this.cookieService.set('jdx', token.substr(token.length / 2, token.length), 365, '/');
            this.cookieService.set('jdd', localStorage.getItem('JD-D'), 1, '/', null, true, "None");
            console.log(this.cookieService.getAll());
        } else {
            this.logout();
        }
    }

    clearCookies() {
        this.cookieService.deleteAll();
    }

    getUser() {
        if (localStorage.getItem('JD-T')) {
            const helper = new JwtHelperService();
            const myRawToken = localStorage.getItem('JD-T');
            this.decodedToken = helper.decodeToken(myRawToken);
            const isExpired = helper.isTokenExpired(myRawToken);
            if (isExpired) {
                this.logout();
                alert("Session Expired");
            }
            this.currentUser = JSON.parse(this.decodedToken.user);
            this.projects = this.getProjects();
            this.canCreate = this.hasAuthority('ISSUE_CREATE');
        }
    }

    getUserTimezone() {
        if (!this.currentUser) this.getUser();
        return this.currentUser ? this.currentUser.timezone : '';
    }

    getProjects() {
        if (!this.currentUser) this.getUser();
        return this.decodedToken ? JSON.parse(this.decodedToken.accpr) : null;
    }

    isSuperAdmin(): boolean {
        if (!this.currentUser) this.getUser();
        return this.currentUser ? this.currentUser.superAdmin : false;
    }

    hasAuthority(code: string) {
        if (this.hasGlobal(code)) return true;
        var hasAuthority = false;
        this.projects.forEach(p => {
            if (p.authorities.includes(code))
                hasAuthority = true;
        });
        return hasAuthority;
    }

    hasGlobal(code: string) {
        if (!this.currentUser) this.getUser();
        if (!this.decodedToken) {
            return false;
        } else {
            if (this.decodedToken.admin) { return true; }
            const g: string[] = this.decodedToken.g;
            return g.includes(code);
        }
    }

    getToken() {
        if (!localStorage.getItem('JD-T')) {
            return '';
        }
        return localStorage.getItem('JD-T');
    }

    retry() {
        this.getUserDetails().subscribe(resp => {
            //if has response
            window.location.reload();
        });
    }

    getCurrentUser() {
        if (!this.currentUser) this.getUser();
        return this.currentUser;
    }

    getUserDetails() {
        if (!this.isLoggedIn()) this.logout();
        this.getFingerPrint();
        return this.http.get<any>(HttpService.getBaseURL() + '/auth/verify?tz=' + Intl.DateTimeFormat().resolvedOptions().timeZone, { observe: 'response' });
    }

    forgot(email: string, token: string, password: string, confirmPassword: string) {
        return this.http.post<any>(HttpService.getBaseURL() + '/auth/forgot', { username: email, token: token, password: password, confirmPassword: confirmPassword });
    }

    register() {
        return this.http.get<any>(HttpService.getBaseURL() + '/auth/register');
    }

    registerUser(data: any) {
        return this.http.post<any>(HttpService.getBaseURL() + '/auth/register', data);
    }

    isLoggedIn() {
        if (!this.currentUser) this.getUser();
        return this.currentUser != null
    }

    verify() {
        return this.isLoggedIn()
    }

    logout() {
        this.cancelPendingRequests();
        this.logoutSubject.next(new Date().getTime());
        this.clearCookies();
        localStorage.clear();
        this.http.get<any>(HttpService.getBaseURL() + '/auth/logout', { observe: 'response' })
            .pipe(map(resp => {
                this.router.navigate(['login']);
            })).subscribe(resp => {
                // logout success, do nothing
            }, error => {
                this.router.navigate(['login']);
            });
    }

    getSetupStatus() {
        return this.http.get<any>(HttpService.getBaseURL() + '/auth/setup');
    }

    setup(data: any) {
        return this.http.post<any>(HttpService.getBaseURL() + '/auth/setup', data);
    }
}

export class Login {
    public username: string;
    public password: string;
    public deviceInfo: string;
}
