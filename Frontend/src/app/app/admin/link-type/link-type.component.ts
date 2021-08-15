import { JDToastrService } from './../../toastr.service';
import { ManageService } from './../manage.service';
import { Relationship } from './../../issue/issue';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-link-type',
  templateUrl: './link-type.component.html',
  styleUrls: ['./link-type.component.css']
})
export class LinkTypeComponent implements OnInit {

  public relationships: Relationship[];
  public relationship: Relationship;
  constructor(private manageService: ManageService, private toastr: JDToastrService) { }

  ngOnInit() {
    this.get();
  }

  get() {
    this.manageService.getRelationships().subscribe(resp => this.relationships = resp);
  }

  openRelationship(r: Relationship) {
    this.relationship = r;
  }

  create() {
    this.relationship = new Relationship();
  }

  save() {
    this.manageService.saveRelationship(this.relationship).subscribe(resp => {
      this.toastr.success('Relationship', ' saved');
      this.get();
      this.cancel();
    });
  }

  cancel() {
    this.relationship = undefined;
  }

  delete() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.manageService.removeRelationship(this.relationship).subscribe(resp => {
          this.toastr.success('Relationship', 'removed');
          this.get();
          this.cancel();
        });
      }
    });
  }
}
