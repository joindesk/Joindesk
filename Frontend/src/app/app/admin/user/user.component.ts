import { JDToastrService } from './../../toastr.service';
import { UserService } from './user.service';
import { Component, OnInit } from '@angular/core';
import { JDUser } from './user';

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})
export class UserComponent implements OnInit {
  user: JDUser;
  slackEnabled = false;
  slackCandidates = [];
  pendingUsers = [];
  showImport = false;
  showPending = false;
  constructor(public userService: UserService, private toastr: JDToastrService) {
    userService.userChange.subscribe(() => {
      this.user = this.userService.fetchUser();
      this.showImport = false;
    });
  }

  ngOnInit() {
    this.get();
    this.userService.slackEnabled().subscribe(resp => {
      this.slackEnabled = resp.enabled;
    });
    this.userService.userChange.subscribe(r => {
      this.get();
    });
  }

  get() {
    this.userService.getUsers();
  }

  getCandidates() {
    if (this.slackEnabled) {
      this.userService.getSlackCandidates().subscribe(resp => {
        this.slackCandidates = resp;
        this.showImport = true;
        this.cancel();
      });
    }
  }

  getPendingUsers() {
    this.userService.getPendingUsers().subscribe(resp => {
      this.pendingUsers = resp;
      this.showPending = true;
      this.cancel();
    });
  }

  approvePendingUser(l: JDUser) {
    this.userService.approvePendingUser(l).subscribe(resp => {
      this.toastr.success("Approved", "User");
      this.getPendingUsers();
    });
  }

  rejectPendingUser(l: JDUser) {
    this.userService.rejectPendingUser(l).subscribe(resp => {
      this.toastr.success("Rejected", "User");
      this.getPendingUsers();
    });
  }


  create() {
    this.user = new JDUser();
    this.user.edit = true;
    this.showImport = false;
    this.user.active = true;
  }

  cancel() {
    if (this.user && this.user.id > 0) {
      this.userService.closeUser();
    } else {
      this.user = undefined;
    }
  }

  cancelEdit() {
    if (this.user && this.user.id > 0) {
      this.user.edit = false;
    } else {
      this.cancel();
    }
  }

  save() {
    if (this.user && this.user.id > 0) {
      this.userService.save(this.user);
    } else {
      this.userService.createUser(this.user);
    }
  }

  importSlackUser(id) {
    this.userService.importSlackUser(id).subscribe(resp => {
      this.toastr.success('Slack Import', 'Imported');
      this.get();
      this.getCandidates();
    });
  }

  getApiToken() {
    this.userService.getApiToken(this.user).subscribe(resp => {
      this.user.apiToken = resp;
    });
  }

  resetApiToken() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.userService.resetApiToken(this.user).subscribe(resp => {
          this.user.apiToken = resp;
          this.toastr.success('API', 'Token resetted');
        });
      }
    });
  }

}
