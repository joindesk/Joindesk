import { AuthenticationService, Login } from './../authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit, OnDestroy, AfterViewInit, NgZone } from '@angular/core';
import { JDUser } from './../../app/admin/user/user';
import { interval, Subscription } from 'rxjs';
import { JDToastrService } from '../../app/toastr.service';
import { JwtHelperService } from '@auth0/angular-jwt';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy, AfterViewInit {
  isBrowser = true;
  model: any = {};
  loading = false;
  otheroptions = false;
  errorMessage = '';
  infoMessage = '';
  returnUrl: string;
  currentLoginUser: JDUser;
  showRegistration = false;
  subscription: Subscription;
  validateForm: FormGroup;
  constructor(
    private authenticationService: AuthenticationService, private fb: FormBuilder,
    private router: Router, private route: ActivatedRoute, private toastr: JDToastrService) {
    this.returnUrl = this.authenticationService.redirectUrl || '/home';
  }

  ngOnInit() {
    this.validateForm = this.fb.group({
      email: [null, [Validators.email, Validators.required]],
      password: [null, [Validators.required]],
      remember: [false]
    });
    if (this.authenticationService.verify()) {
      this.router.navigateByUrl(this.returnUrl);
    }
    this.authenticationService.register().subscribe(r => {
      this.showRegistration = r.allowed;
      if (!r.setup) {
        alert("Setup not complete");
        this.router.navigateByUrl("/setup");
      }
      this.loading = false;
    }, e => alert("Cannot connect to server"));
  }

  //Allow only browsers which are not IE
  browserCheck() {
    const inBrowser = typeof window !== 'undefined'
    const UA = inBrowser && window.navigator.userAgent.toLowerCase()
    const isIE = UA && /msie|trident/.test(UA)
    const isIE9 = UA && UA.indexOf('msie 9.0') > 0
    const isEdge = UA && UA.indexOf('edge/') > 0
    const isAndroid = (UA && UA.indexOf('android') > 0)
    const isIOS = (UA && /iphone|ipad|ipod|ios/.test(UA))
    const isChrome = UA && /chrome\/\d+/.test(UA) && !isEdge
    const isPhantomJS = UA && /phantomjs/.test(UA)
    const isFF = UA && UA.match(/firefox\/(\d+)/)
    this.isBrowser = inBrowser && !isIE && !isIE9
  }

  ngAfterViewInit() {
    this.authenticationService.getFingerPrint();
    this.authenticationService.authChange.subscribe(resp => {
      if (this.authenticationService.isLoggedIn())
        this.router.navigateByUrl(this.returnUrl);
    });
  }

  getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  ngOnDestroy() {
    if (this.subscription)
      this.subscription.unsubscribe();
  }

  checkUser() {
    this.loading = true;
    this.errorMessage = '';
    this.infoMessage = '';
    this.authenticationService.checkUser(this.model.username, this.model.mode).subscribe(resp => {
      if (resp.success) {
        this.currentLoginUser = JSON.parse(resp.user);
        this.model.mode = resp.otpMode;
        if (resp.info) {
          this.infoMessage = resp.info;
          this.toastr.infoMessage(resp.info);
        }
        if (resp.error) {
          this.errorMessage = resp.error;
          this.toastr.errorMessage(resp.error);
        }
      } else {
        this.errorMessage = resp.error;
        this.toastr.errorMessage(resp.error);
      }
      this.loading = false;
    })
  }

  validate() {
    for (const i in this.validateForm.controls) {
      this.validateForm.controls[i].markAsDirty();
      this.validateForm.controls[i].updateValueAndValidity();
    }
    if (this.validateForm.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.infoMessage = '';
    this.authenticationService.login(this.validateForm.controls.email.value, this.validateForm.controls.password.value, this.validateForm.controls.remember.value)
      .subscribe(
        resp => {
          if (resp.success) {
            //var tokenDetails = JSON.parse(atob(resp.details.split('.')[1]));
            const helper = new JwtHelperService();
            const decodedToken = helper.decodeToken(resp.details);
            var user = JSON.parse(decodedToken.user);
            localStorage.setItem('currentUser', decodedToken.user);
            this.authenticationService.currentUser = user;
            this.router.navigateByUrl((this.authenticationService.redirectUrl === '/login') ?
              '/' : this.authenticationService.redirectUrl);
          } else {
            if (resp.message) {
              this.errorMessage = resp.message;
              this.toastr.errorMessage(this.errorMessage);
            } else {
              this.errorMessage = 'Invalid User / Password';
              this.toastr.errorMessage(this.errorMessage);
            }
          }
          this.loading = false;
        },
        error => {
          // this.alertService.error(error);
          console.log('login error');
          this.loading = false;
        });
  }

  changeMode(mode) {
    this.model.mode = mode;
    this.checkUser();
    this.otheroptions = false;
  }

}
