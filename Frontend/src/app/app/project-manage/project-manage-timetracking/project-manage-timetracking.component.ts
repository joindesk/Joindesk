import { ActivatedRoute } from '@angular/router';
import { JDToastrService } from './../../toastr.service';
import { ProjectService } from './../project.service';
import { Project, TimeTracking } from './../project';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-project-manage-timetracking',
  templateUrl: './project-manage-timetracking.component.html',
  styleUrls: ['./project-manage-timetracking.component.css']
})
export class ProjectManageTimetrackingComponent implements OnInit {
  timetracking: TimeTracking;
  private projectKey;
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.projectService.getProjectTimeTrackingSettings(this.projectKey).subscribe(resp => {
      this.timetracking = resp;
    });
  }

  save() {
    this.timetracking.enabled = !this.timetracking.enabled;
    this.projectService.saveProjectTimeTrackingSettings(this.projectKey, this.timetracking).subscribe(resp => {
      this.timetracking = resp;
    });
  }

}
