import { ManageService } from './../manage.service';
import { Component, OnInit } from '@angular/core';
import { Resolution } from '../../issue/issue';
import { JDToastrService } from '../../toastr.service';

@Component({
  selector: 'app-resolution',
  templateUrl: './resolution.component.html',
  styleUrls: ['./resolution.component.css']
})
export class ResolutionComponent implements OnInit {
  public resolutions: Resolution[];
  public resolution: Resolution;
  constructor(private manageService: ManageService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.get();
  }

  get() {
    this.manageService.getResolutions().subscribe(resp => this.resolutions = resp);
  }

  openResolution(r: Resolution) {
    this.resolution = r;
  }

  create() {
    this.resolution = new Resolution();
  }

  save() {
    this.manageService.saveResolution(this.resolution).subscribe(resp => {
      this.toastr.success('Resolution', ' saved');
      this.get();
      this.cancel();
    });
  }

  cancel() {
    this.resolution = undefined;
  }

}
