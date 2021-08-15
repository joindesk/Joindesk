import { Component, OnInit } from "@angular/core";
import { Group } from "../group";
import { JDUser }from "../../admin/user/user";
import { ActivatedRoute, Router } from "@angular/router";
import { JDToastrService } from "../../toastr.service";
import { GroupService } from "../group.service";

@Component({
  selector: "app-project-manage-group",
  templateUrl: "./project-manage-group.component.html",
  styleUrls: ["./project-manage-group.component.css"]
})
export class ProjectManageGroupComponent implements OnInit {
  public projectKey;
  public group: Group;
  public groups: Group[];
  public members: JDUser[];
  public authorityCodes: string[];
  public fields = {
    member: "",
    authority: ""
  };
  constructor(
    private route: ActivatedRoute,
    private groupService: GroupService,
    private toastr: JDToastrService
  ) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get(
      "projectKey"
    );
    this.getGroups();
    this.groupService.getMembers().subscribe(
      m => {
        this.members = m;
      },
      error => console.log(error)
    );
    this.groupService.getAuthorityCodes().subscribe(
      ac => {
        this.authorityCodes = ac;
      },
      error => console.log(error)
    );
  }

  getGroups() {
    this.groupService.getGroups(this.projectKey).subscribe(resp => {
      this.groups = resp;
    });
  }

  openGroup(groupID: number) {
    this.groupService.getGroup(this.projectKey, groupID).subscribe(
      resp => {
        this.group = resp;
      },
      error => {
        console.log(error);
      }
    );
  }

  compareFn(user1: any, user2: any) {
    return user1 && user2 ? user1.id === user2.id : user1 === user2;
  }

  save(allUsers) {
    this.group.allUsers = allUsers.checked;
    if (this.group.id > 0) {
      this.groupService.save(this.projectKey, this.group).subscribe(
        resp => {
          this.group = resp;
          this.toastr.success("Group", " saved");
          this.cancel();
          this.getGroups();
        },
        error => console.error(error)
      );
    } else {
      this.groupService.createGroup(this.projectKey, this.group).subscribe(
        resp => {
          this.group = resp;
          this.toastr.success("Group", " created");
          this.cancel();
          this.getGroups();
          this.openGroup(resp.id);
        },
        error => console.error(error)
      );
    }
  }

  create() {
    this.group = new Group();
    this.group.users = [];
    this.group.authorityCodes = [];
  }

  cancel() {
    if (this.group.id > 0) {
      this.groupService.closeGroup();
      this.group = undefined;
    } else {
      this.group = undefined;
    }
  }

  delete() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.groupService.remove(this.projectKey, this.group).subscribe(resp => {
          this.toastr.success("Group", "removed");
          this.getGroups();
          this.cancel();
        });
      }
    });
  }
}
