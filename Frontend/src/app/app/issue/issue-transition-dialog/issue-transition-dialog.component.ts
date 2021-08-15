import { Component, OnInit, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-issue-transition-dialog',
  templateUrl: './issue-transition-dialog.component.html',
  styleUrls: ['./issue-transition-dialog.component.css']
})
export class IssueTransitionDialogComponent implements OnInit {
  showLoading = true;
  innerWidth: any;
  innerHeight: any;
  data: any;
  fields: any[] = [];
  constructor(@Inject(MAT_DIALOG_DATA) public d: any, private dialogRef: MatDialogRef<IssueTransitionDialogComponent>, ) {
    this.innerWidth = (window.innerWidth / 3) * 2;
    this.innerHeight = (window.innerHeight / 3) * 2;
    this.data = d
  }

  ngOnInit() {
    //To have field modal to fill required field before transition
    console.log("from dialog");
    console.log(this.data);
    this.data.error.fields.forEach(element => {
      let f = {
        label: element.field,
        value: undefined,
        values: JSON.parse(element.values)
      };
      this.fields.push(f);
    });
  }

  makeTransition() {
    this.data.fields = this.fields;
    this.dialogRef.close(this.data);
  }

}
