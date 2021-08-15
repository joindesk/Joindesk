import { JDComponentService } from './jdcomponent.service';
import { JDComponent } from './jdcomponent';
import { ActivatedRoute } from '@angular/router';
import { JDToastrService } from './../../toastr.service';
import { OnInit, Component } from '@angular/core';

@Component({
  selector: 'app-jdcomponent',
  templateUrl: './jdcomponent.component.html',
  styleUrls: ['./jdcomponent.component.css']
})
export class JDCntrlrComponent implements OnInit {
  private projectKey;
  public components: JDComponent[];
  public component: JDComponent;
  constructor(private componentService: JDComponentService, private toastr: JDToastrService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.projectKey = this.route.snapshot.parent.parent.paramMap.get('projectKey');
    this.getComponents();
  }

  getComponents() {
    this.componentService.getAll(this.projectKey).subscribe(resp => {
      this.components = (resp || []).sort((a: any, b: any) => a.id < b.id ? -1 : 1);
    });
  }

  open(id: number) {
    this.componentService.get(this.projectKey, id).subscribe(resp => {
      this.component = resp;
    });
  }

  save() {
    this.componentService.save(this.projectKey, this.component).subscribe(resp => {
      this.component = resp;
      this.getComponents();
      this.toastr.success('Component', 'saved');
    }, error => console.error(error));
  }

  create() {
    this.component = new JDComponent;
    this.component.edit = true;
  }

  cancel() {
    if (this.component.id > 0) {
      this.open(this.component.id);
    } else {
      this.component = undefined;
    }
  }

  compareFn(a: any, b: any) {
    return a && b ? a.id === b.id : a === b;
  }


  delete() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.componentService.delete(this.projectKey, this.component).subscribe(resp => {
          this.toastr.success('Component', 'removed');
          this.cancel();
          this.getComponents();
        });
      }
    });
  }

}
