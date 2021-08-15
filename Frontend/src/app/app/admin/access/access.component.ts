import { Access } from './../../project-manage/access';
import { ManageService } from './../manage.service';
import { JDToastrService } from './../../toastr.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-access',
  templateUrl: './access.component.html',
  styleUrls: ['./access.component.css']
})
export class AccessComponent implements OnInit {
  public rules: Access[];
  public rule: Access;
  constructor(private manageService: ManageService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.get();
  }

  get() {
    this.manageService.getAccess().subscribe(resp => this.rules = resp);
  }

  create() {
    this.rule = new Access();
  }

  save(e, a, w) {
    this.rule.enabled = e.checked; this.rule.apiOnly = a.checked; this.rule.whiteList = w.checked;
    this.manageService.saveAccess(this.rule).subscribe(resp => {
      this.toastr.success('Rule', ' saved');
      this.get();
      this.cancel();
    });
  }

  open(r) {
    this.rule = r;
    this.rule.edit = true;
  }

  cancel() {
    this.rule = undefined;
  }

  delete(r: Access) {
    this.manageService.removeAccess(r).subscribe(resp => {
      this.toastr.success('Rule', ' removed');
      this.get();
      this.cancel();
    });
  }

}
