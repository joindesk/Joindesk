import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from '../authentication.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  model: any = {};
  loading = false;
  registerAllowed = true;
  errorMessage = '';
  infoMessage = '';
  constructor(private authenticationService: AuthenticationService,
    private router: Router) { }

  ngOnInit() {
    this.authenticationService.register().subscribe(r => {
      this.registerAllowed = r.allowed;
      if (!r.allowed) {
        this.errorMessage = "Registration disabled, Please contact Administrator";
      }
    });
  }

  register() {
    this.loading = true;
    this.errorMessage = '';
    this.authenticationService.registerUser(this.model).subscribe(resp => {
      if (resp.success) {
        this.infoMessage = "Registered, Please check email for further information";
        if (resp.error)
          this.errorMessage = resp.error;
      } else {
        this.errorMessage = resp.error;
      }
      this.loading = false;
    }, err => {
      this.loading = false;
    })
  }

}
