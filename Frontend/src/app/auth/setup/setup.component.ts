import { AuthenticationService, Login } from './../authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit, OnDestroy, AfterViewInit, NgZone } from '@angular/core';
import { JDUser } from './../../app/admin/user/user';
import { Subscription } from 'rxjs';
import { JDToastrService } from '../../app/toastr.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-setup',
  templateUrl: './setup.component.html',
  styleUrls: ['./setup.component.css']
})
export class SetupComponent implements OnInit {
  isBrowser = true;
  model: any = {};
  loading = true;
  otheroptions = false;
  errorMessage = '';
  infoMessage = '';
  returnUrl: string;
  currentLoginUser: JDUser;
  showSetup = false;
  timezones = [];
  validateForm: FormGroup;
  constructor(
    private authenticationService: AuthenticationService, private fb: FormBuilder,
    private router: Router, private route: ActivatedRoute, private toastr: JDToastrService) {
  }

  ngOnInit() {
    if (this.authenticationService.verify()) {
      this.router.navigateByUrl(this.returnUrl);
    }
    this.authenticationService.getSetupStatus().subscribe(r => {
      this.showSetup = !r.status;
      this.timezones = r.timezones;
      this.loading = false;
      if (this.timezones.includes(Intl.DateTimeFormat().resolvedOptions().timeZone))
        this.model.timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    });
    this.model.url = window.location.origin;
    this.model.startTime = "09:00";
    this.model.endTime = "18:00";
    this.validateForm = this.fb.group({
      email: [null, [Validators.email, Validators.required]],
      password: [null, [Validators.required]],
      url: [null, [Validators.required]],
      timezone: [null, [Validators.required]],
      startTime: [null, [Validators.required]],
      endTime: [null, [Validators.required]]
    });
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

  saveSetup() {
    for (const i in this.validateForm.controls) {
      this.validateForm.controls[i].markAsDirty();
      this.validateForm.controls[i].updateValueAndValidity();
    }
    console.log(this.validateForm);
    if (this.validateForm.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.infoMessage = '';

    this.authenticationService.setup(this.model).subscribe(resp => {
      if (resp.success) {
        this.toastr.successMessage("Configuration saved, login to continue");
        this.router.navigateByUrl("login");
      } else {
        this.errorMessage = resp.error;
        this.toastr.errorMessage(resp.error);
      }
      this.loading = false;
    })
  }

}
