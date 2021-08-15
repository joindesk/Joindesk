import { JDUser } from './../user/user';
import { GroupService } from './../../project-manage/group.service';
import { JDToastrService } from './../../toastr.service';
import { ManageService } from './../manage.service';
import { Group } from './../../project-manage/group';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-global-group',
  templateUrl: './global-group.component.html',
  styleUrls: ['./global-group.component.css']
})
export class GlobalGroupComponent implements OnInit {

  public groups: Group[];
  public group: Group;
  public members: JDUser[];
  public authorityCodes: string[];
  constructor(private manageService: ManageService, private toastr: JDToastrService,
    private groupService: GroupService) { }

  ngOnInit() {
    this.get();
    this.groupService.getMembers().subscribe(m => { this.members = m; }, error => console.log(error));
    this.groupService.getAuthorityCodes().subscribe(ac => { this.authorityCodes = ac; }, error => console.log(error));
  }

  get() {
    this.manageService.getGlobalGroups().subscribe(resp => this.groups = resp);
  }

  openGroup(r: Group) {
    this.group = r;
  }

  compareFn(user1: any, user2: any) {
    return user1 && user2 ? user1.id === user2.id : user1 === user2;
  }

  create() {
    this.group = new Group();
    this.group.users = [];
    this.group.authorityCodes = [];
  }

  save() {
    this.manageService.saveGlobalGroup(this.group).subscribe(resp => {
      this.toastr.success('Group', ' saved');
      this.get();
      this.cancel();
    });
  }

  cancel() {
    this.group = undefined;
  }

  delete() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.manageService.removeGlobalGroup(this.group).subscribe(resp => {
          this.toastr.success('Group', 'removed');
          this.get();
          this.cancel();
        });
      }
    });
  }

}
