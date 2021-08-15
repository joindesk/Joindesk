import { JDToastrService } from './../../toastr.service';
import { ProjectService } from './../project.service';
import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { Project, GitRepository } from '../project';

@Component({
  selector: 'app-project-manage-repository',
  templateUrl: './project-manage-repository.component.html',
  styleUrls: ['./project-manage-repository.component.css']
})
export class ProjectManageRepositoryComponent implements OnInit {
  private projectKey;
  project: Project;
  repo: GitRepository;
  repos: GitRepository[];
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.projectService.getProjectByKey(this.projectKey).subscribe(resp => {
      this.project = resp;
      this.getrepos();
    });
  }

  getrepos() {
    this.projectService.getProjectRepositorys(this.project.key).subscribe(resp => {
      this.repos = resp;
    });
  }

  open(repo: GitRepository) {
    this.repo = JSON.parse(JSON.stringify(repo));
  }

  save(f) {
    this.projectService.saveProjectGitRepository(this.projectKey, this.repo).subscribe(resp => {
      this.repo = resp;
      this.getrepos();
    }, error => console.error(error));
  }

  create() {
    this.repo = new GitRepository;
    this.repo.edit = true;
  }

  cancel() {
    if (this.repo.id > 0) {
      this.repo = undefined;
    } else {
      this.repo = undefined;
    }
  }

}
