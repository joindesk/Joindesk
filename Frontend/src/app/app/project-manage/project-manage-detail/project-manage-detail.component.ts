import { ProjectService } from './../project.service';
import { Component, OnInit, ViewChild } from '@angular/core';
import { JDToastrService } from '../../toastr.service';
import { ActivatedRoute } from '@angular/router';
import { Project } from '../project';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-project-manage-detail',
  templateUrl: './project-manage-detail.component.html',
  styleUrls: ['./project-manage-detail.component.css']
})
export class ProjectManageDetailComponent implements OnInit {
  private projectKey;
  project: Project;
  @ViewChild('form', { static: false }) form: NgForm;
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.projectService.getProjectByKey(this.projectKey).subscribe(resp => {
      this.project = resp;
    });
  }

  updateProject() {
    if (!this.form.dirty) return
    this.projectService.save(this.project).subscribe(resp => {
      if (resp.status === 200) {
        this.project = resp.body;
        this.toastr.success('Project', 'updated');
        this.form.form.markAsPristine()
      } else {
        this.toastr.error('Project', resp.body);
      }
    });
  }
}
