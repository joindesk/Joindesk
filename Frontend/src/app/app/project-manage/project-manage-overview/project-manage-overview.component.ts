import { ProjectService } from './../project.service';
import { Component, OnInit } from '@angular/core';
import { JDToastrService } from '../../toastr.service';
import { ActivatedRoute } from '@angular/router';
import { Project } from '../project';

@Component({
  selector: 'app-project-manage-overview',
  templateUrl: './project-manage-overview.component.html',
  styleUrls: ['./project-manage-overview.component.css']
})
export class ProjectManageOverviewComponent implements OnInit {
  private projectKey;
  project: Project;
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.projectService.getProjectByKey(this.projectKey).subscribe(resp => {
      this.project = resp;
    });
  }

}
