import { Component, OnInit } from '@angular/core';
import { Project } from '../../project-manage/project';
import { ProjectService } from '../../project-manage/project.service';
import { JDToastrService } from '../../toastr.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Version } from '../../issue/issue';

@Component({
  selector: 'app-release',
  templateUrl: './release.component.html',
  styleUrls: ['./release.component.css']
})
export class ReleaseComponent implements OnInit {
  loading = false;
  projectKey;
  project: Project;
  version: Version;
  versions: Version[];
  showVersionModal = false;
  showVersionReleaseModal = false;
  constructor(private projectService: ProjectService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.paramMap.get('projectKey');
    this.projectService.getProjectByKey(this.projectKey).subscribe(resp => {
      this.project = resp;
      this.getVersions();
    });
  }

  getVersions() {
    this.loading = true;
    this.projectService.getProjectVersions(this.project.key).subscribe(resp => {
      this.versions = resp;
      this.loading = false;
    });
  }

  open(id: number) {
    this.projectService.getProjectVersion(this.project.key, id).subscribe(resp => {
      this.version = resp;
      this.showVersionModal = true;
    });
  }

  save() {
    this.projectService.saveProjectVersion(this.projectKey, this.version).subscribe(resp => {
      this.version = resp;
      this.getVersions();
      this.toastr.successMessage("Version saved");
      this.cancel()
    }, error => console.error(error));
  }

  create() {
    this.version = new Version;
    this.version.edit = true;
    this.showVersionModal = true;
  }

  cancel() {
    this.version = undefined;
    this.showVersionModal = false;
    this.showVersionReleaseModal = false;
  }

  release(v: Version, status: boolean) {
    if (v.id) {
      if (!v.releaseDate && status) {
        this.version = v;
        this.showVersionReleaseModal = true;
        return;
      }
      console.log(v);
      v.released = status;
      this.projectService.releaseProjectVersion(this.projectKey, v).subscribe(resp => {
        this.toastr.successMessage("Version updated");
        this.getVersions();
        this.cancel();
      })
    }
  }

  delete(v: Version) {
    this.toastr.confirm('').then(result => {
      if (result.value) {
        this.projectService.removeProjectVersion(this.projectKey, v).subscribe(resp => {
          this.toastr.successMessage("Version removed");
          this.getVersions();
        })
      }
    });
  }

  parseDate(dateString: string): Date {
    if (dateString) {
      return new Date(dateString);
    } else {
      return null;
    }
  }

  searchByVersion(v: string) {
    return "/project/" + this.projectKey + '?q=' + 'version:' + v;
  }

  searchByVersionUnResolved(v: string) {
    return "/project/" + this.projectKey + '?q=' + 'version:' + v;
  }

  sort(sort: { key: string; value: string }): void {
    this.loading = true;
    const vCopy = JSON.parse(JSON.stringify(this.versions));
    this.versions = [];
    if (sort.key && sort.value) {
      this.versions = vCopy.sort((a, b) =>
        sort.value === 'ascend' ? a[sort.key!] > b[sort.key!] ? 1 : -1 : b[sort.key!] > a[sort.key!] ? 1 : -1
      );
    } else {
      this.versions = vCopy;
    }
    this.loading = false;
  }

  getPercentage(it: Version) {
    const a = it.totalIssues
    const b = it.totalResolved
    if (b == 0 || a == 0) return 0;
    return Math.round((100 * b) / a);
  }

}
