import { JDToastrService } from './../../toastr.service';
import { ProjectService } from './../project.service';
import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { Project } from '../project';
import { Version } from '../../issue/issue';

@Component({
  selector: 'app-project-manage-version',
  templateUrl: './project-manage-version.component.html',
  styleUrls: ['./project-manage-version.component.css']
})
export class ProjectManageVersionComponent implements OnInit {
  private projectKey;
  project: Project;
  version: Version;
  versions: Version[];
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.projectService.getProjectByKey(this.projectKey).subscribe(resp => {
      this.project = resp;
      this.getVersions();
    });
  }

  getVersions() {
    this.projectService.getProjectVersions(this.project.key).subscribe(resp => {
      this.versions = resp;
    });
  }

  open(id: number) {
    this.projectService.getProjectVersion(this.project.key, id).subscribe(resp => {
      this.version = resp;
    });
  }

  save(f) {
    this.projectService.saveProjectVersion(this.projectKey, this.version).subscribe(resp => {
      this.version = resp;
      this.getVersions();
    }, error => console.error(error));
  }

  create() {
    this.version = new Version;
    this.version.edit = true;
  }

  cancel() {
    if (this.version.id > 0) {
      this.version = undefined;
    } else {
      this.version = undefined;
    }
  }

  parseDate(dateString: string): Date {
    if (dateString) {
      return new Date(dateString);
    } else {
      return null;
    }
  }

}
