import { AuthenticationService, Login } from './../authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit, OnDestroy, AfterViewInit, NgZone } from '@angular/core';
import { JDUser } from './../../app/admin/user/user';
import { interval, Subscription } from 'rxjs';
import { JDToastrService } from '../../app/toastr.service';
import { JwtHelperService } from '@auth0/angular-jwt';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-forgot',
  templateUrl: './forgot.component.html',
  styleUrls: ['./forgot.component.css']
})
export class ForgotComponent implements OnInit {
  loading = false;
  validateForm: FormGroup;
  constructor(private authenticationService: AuthenticationService, private fb: FormBuilder,
    private router: Router, private route: ActivatedRoute, private toastr: JDToastrService) { }

  ngOnInit() {
    this.validateForm = this.fb.group({
      email: [null, [Validators.email, Validators.required]],
      token: [null, []],
      password: [null, []],
      confirmPassword: [null, []]
    });
  }

  submit(type: string) {
    this.loading = true;
    const reset = type == "reset";
    this.validateForm.controls["token"].setValidators(reset ? [Validators.required] : []);
    this.validateForm.controls["password"].setValidators(reset ? [Validators.required, Validators.minLength(8), Validators.maxLength(50)] : []);
    this.validateForm.controls["confirmPassword"].setValidators(reset ? [Validators.required, Validators.minLength(8), Validators.maxLength(50), this.confirmationValidator] : []);
    for (const i in this.validateForm.controls) {
      this.validateForm.controls[i].markAsDirty();
      this.validateForm.controls[i].updateValueAndValidity();
    }
    this.loading = false;
    if (this.validateForm.invalid) return;
    this.loading = true;
    this.authenticationService.forgot(this.validateForm.controls.email.value, reset ? this.validateForm.controls.token.value : '',
      reset ? this.validateForm.controls.password.value : '', reset ? this.validateForm.controls.confirmPassword.value : '')
      .subscribe(
        resp => {
          if (resp.success) {
            this.toastr.successMessage(resp.message);
            if (resp.changed)
              this.router.navigateByUrl("/login");
          } else {
            this.toastr.errorMessage(resp.message);
          }
          this.loading = false;
        },
        error => {
          // this.alertService.error(error);
          console.log('login error');
          this.loading = false;
        });
  }

  confirmationValidator = (control: FormControl): { [s: string]: boolean } => {
    if (!control.value) {
      return { required: true };
    } else if (control.value !== this.validateForm.controls.password.value) {
      return { confirm: true, error: true };
    }
    return {};
  };

}
