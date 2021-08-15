import { MyaccountService } from './myaccountservice.service';
import { Component, OnInit, AfterViewInit } from '@angular/core';
import { JDToastrService } from '../toastr.service';
import { FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';

@Component({
  selector: 'app-myaccount',
  templateUrl: './myaccount.component.html',
  styleUrls: ['./myaccount.component.css']
})
export class MyaccountComponent implements OnInit {
  selectedTab = 0;
  mfaLoading = false;
  model = {
    editPassword: false,
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: '',
    edit: false
  };
  isChangePwdLoading = false;
  displayedColumns: string[] = ['device', 'created', 'lastused', 'action'];
  mfaForm!: FormGroup;
  changePwdForm: FormGroup;
  constructor(public myAccountService: MyaccountService, private toastr: JDToastrService,
    private fb: FormBuilder) { }

  ngOnInit() {
    this.myAccountService.getSessions();
    this.myAccountService.get();
    this.mfaForm = this.fb.group({
      mfaCode: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(6), Validators.pattern("^[0-9]*$")]],
    });
    this.changePwdForm = this.fb.group({
      oldPassword: [null, [Validators.required, Validators.minLength(8), Validators.maxLength(50)]],
      password: [null, [Validators.required, Validators.minLength(8), Validators.maxLength(50)]],
      confirmPassword: [null, [Validators.required, Validators.minLength(8), Validators.maxLength(50), this.confirmationValidator]]
    });
  }

  changePassword(): void {
    for (const i in this.changePwdForm.controls) {
      this.changePwdForm.controls[i].markAsDirty();
      this.changePwdForm.controls[i].updateValueAndValidity();
    }
    if (this.changePwdForm.valid) {
      this.isChangePwdLoading = true;
      this.myAccountService.changePassword(this.changePwdForm.controls.oldPassword.value,
        this.changePwdForm.controls.password.value, this.changePwdForm.controls.confirmPassword.value).subscribe(resp => {
          if (resp.success) {
            this.toastr.successMessage("Password changed successfully");
            this.changePwdForm.reset();
          } else {
            this.toastr.errorMessage(resp.error);
          }
          this.isChangePwdLoading = false;
        });
    }
  }

  confirmationValidator = (control: FormControl): { [s: string]: boolean } => {
    if (!control.value) {
      return { required: true };
    } else if (control.value !== this.changePwdForm.controls.password.value) {
      return { confirm: true, error: true };
    }
    return {};
  };

  attach(event) {
    const fileList: FileList = event.target.files;
    this.myAccountService.updatePic(fileList).subscribe(resp => {
      this.toastr.success('Profile picture', 'Updated');
    });
  }

  requestOTP(slackUsername) {
    if (slackUsername === '') { this.toastr.error('Slack', 'Enter slackusername'); return; }
    this.myAccountService.slackConnectInit(slackUsername).subscribe(resp => {
      this.toastr.success('Slack', 'OTP sent');
    });
  }

  activateMfa() {
    for (const i in this.mfaForm.controls) {
      this.mfaForm.controls[i].markAsDirty();
      this.mfaForm.controls[i].updateValueAndValidity();
    }
    if (this.mfaForm.valid) {
      this.mfaLoading = true;
      this.myAccountService.activateMfa(+this.mfaForm.get('mfaCode').value).subscribe(resp => {
        this.toastr.success('Authenticator', 'Linked');
        this.myAccountService.get();
        this.mfaLoading = false;
      }, error => this.mfaLoading = false);
    }

  }

  disconnectMFA() {
    this.mfaLoading = true;
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.myAccountService.deactivateMfa().subscribe(resp => {
          this.toastr.success('Authenticator', 'Unlinked');
          this.myAccountService.get();
          this.mfaLoading = false;
        }, error => this.mfaLoading = false);
      }
      this.mfaLoading = false
    });
  }

  connectSlack(slackUsername, slackToken) {
    if (slackUsername === '') { this.toastr.error('Slack', 'Enter slackusername'); return; }
    if (slackToken === '') { this.toastr.error('Slack', 'Enter slack Token'); return; }
    this.myAccountService.slackConnect(slackUsername, slackToken).subscribe(resp => {
      this.toastr.success('Slack', 'Connected');
      this.myAccountService.get();
    });
  }

  disconnectSlack() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.myAccountService.slackDisconnect().subscribe(resp => {
          this.toastr.success('Slack', 'Disconnected');
          this.myAccountService.get();
        });
      }
    });
  }

  setPreferredAuth(type) {
    this.myAccountService.setPreferredAuth(type).subscribe(resp => {
      this.toastr.success('Preference', 'Updated');
      this.myAccountService.get();
    });
  }

  changeNotificationPref(ele, $event) {
    this.myAccountService.updateNotificationPref(ele, $event.checked).subscribe(resp => {
      this.toastr.success('Preference', 'Updated');
    });
  }

  generatePic() {
    this.myAccountService.generatePic().subscribe(resp => {
      this.toastr.success('Pic', 'Changed');
      this.myAccountService.get();
    });
  }

  removeAvatar() {
    this.myAccountService.removeAvatar().subscribe(resp => {
      this.toastr.success('Pic', 'Removed');
      this.myAccountService.get();
    });
  }

  checkPassStrength(pass) {
    console.log(pass);
    var score = this.scorePassword(pass);
    if (score > 80)
      return "strong";
    if (score > 60)
      return "good";
    if (score >= 30)
      return "weak";
    return "";
  }

  scorePassword(pass) {
    let score = 0;
    if (!pass)
      return score;

    // award every unique letter until 5 repetitions
    var letters = new Object();
    for (var i = 0; i < pass.length; i++) {
      letters[pass[i]] = (letters[pass[i]] || 0) + 1;
      score += 5.0 / letters[pass[i]];
    }

    // bonus points for mixing it up
    var variations = {
      digits: /\d/.test(pass),
      lower: /[a-z]/.test(pass),
      upper: /[A-Z]/.test(pass),
      nonWords: /\W/.test(pass),
    }

    var variationCount = 0;
    for (var check in variations) {
      variationCount += (variations[check] == true) ? 1 : 0;
    }
    score += (variationCount - 1) * 10;

    return score;
  }
}
