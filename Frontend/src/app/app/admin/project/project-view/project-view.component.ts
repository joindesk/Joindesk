import { ProjectService } from './../../../project-manage/project.service';
import { Project, SlackChannel } from './../../../project-manage/project';
import { JDToastrService } from './../../../toastr.service';
import { Component, OnInit, Input } from '@angular/core';
import { UserService } from '../../user/user.service';
import { NzDrawerRef } from 'ng-zorro-antd';

@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.css']
})
export class ProjectViewComponent implements OnInit {
  @Input() projectID = 0;
  public project: Project;
  @Input() public channels: SlackChannel[];
  @Input() slackEnabled = false;
  constructor(private userService: UserService,
    private drawerRef: NzDrawerRef<string>,
    private projectService: ProjectService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.open();
    this.userService.getUsers();
  }

  open() {
    if (this.projectID <= 0) {
      this.project = new Project();
      this.project.edit = true;
      this.project.active = true;
    } else {
      this.openProject();
    }
  }

  openProject() {
    this.projectService.getProject(+this.projectID).subscribe(resp => {
      this.project = resp;
    }, error => {
      console.log(error);
    });
  }

  compareFn(user1: any, user2: any) {
    return user1 && user2 ? user1.id === user2.id : user1 === user2;
  }

  save() {
    if (this.project.id > 0) {
      this.projectService.save(this.project).subscribe(resp => {
        if (resp.status === 200) {
          this.project = resp.body;
          this.projectService.getProjects();
        } else {
          alert('Error');
        }
      }, error => console.error(error));
    } else {
      this.projectService.createProject(this.project).subscribe(resp => {
        if (resp.status === 200) {
          this.project = resp.body;
          this.projectService.getProjects();
          this.close();
        } else {
          alert('Error');
        }
      }, error => {
        if (error.error) {
          this.toastr.error('Error', error.error.error);
        }
      });
    }
  }

  cancel() {
    (this.project.id > 0) ? this.project.edit = false : this.close();
  }

  close(): void {
    this.drawerRef.close(this.project.key);
  }

}
