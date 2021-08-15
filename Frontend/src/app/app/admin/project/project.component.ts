import { ProjectService } from './../../project-manage/project.service';
import { Component, OnInit } from '@angular/core';
import { JDToastrService } from '../../toastr.service';
import { Project, SlackChannel } from '../../project-manage/project';
import { NzDrawerService } from 'ng-zorro-antd';
import { ProjectViewComponent } from './project-view/project-view.component';

@Component({
  selector: 'app-project',
  templateUrl: './project.component.html',
  styleUrls: ['./project.component.css']
})
export class ProjectComponent implements OnInit {
  public project: Project;
  public channels: SlackChannel[];
  slackEnabled = false;
  constructor(public projectService: ProjectService, private drawerService: NzDrawerService) { }

  ngOnInit() {
    this.projectService.getProjects();
    this.projectService.slackEnabled().subscribe(r => {
      this.slackEnabled = r.enabled;
      if (this.slackEnabled)
        this.projectService.getChannels().subscribe(r => this.channels = r)
    });
  }

  open(projectID: number, name: string): void {
    const drawerRef = this.drawerService.create<ProjectViewComponent, { projectID: number, channels: SlackChannel[], slackEnabled: boolean }, string>({
      nzTitle: name,
      nzWidth: 600,
      nzContent: ProjectViewComponent,
      nzContentParams: {
        projectID: projectID,
        channels: this.channels,
        slackEnabled: this.slackEnabled
      }
    });

    drawerRef.afterClose.subscribe(data => {
      this.projectService.getProjects();
    });
  }
}
