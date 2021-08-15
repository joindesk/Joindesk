import { JDToastrService } from './../../toastr.service';
import { ManageService } from './../manage.service';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.css']
})
export class ConfigComponent implements OnInit {
  config: any;
  constructor(private manageService: ManageService, private toastr: JDToastrService,
    private route: ActivatedRoute) { }

  ngOnInit() {
    this.get();
    this.route.queryParams.subscribe(qParams => {
      if (qParams["slack"])
        this.toastr.successMessage("Slack connected successfully")
    });
  }

  get() {
    this.manageService.getAppConfig().subscribe(resp => this.config = resp);
  }

  save() {
    this.manageService.saveAppConfig(this.config).subscribe(resp => {
      this.toastr.success('Configuration', ' saved');
      this.get();
    });
  }

}
