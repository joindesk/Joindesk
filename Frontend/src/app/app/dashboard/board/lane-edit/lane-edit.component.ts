import { BoardService } from './../board.service';
import { Lane } from './../board';
import { MatDialog, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Component, OnInit, Inject } from '@angular/core';
import { JDToastrService } from '../../../toastr.service';

@Component({
  selector: 'app-lane-edit',
  templateUrl: './lane-edit.component.html',
  styleUrls: ['./lane-edit.component.css']
})
export class LaneEditComponent implements OnInit {
  lane: Lane;
  constructor(@Inject(MAT_DIALOG_DATA) public data: { lane: Lane, projectKey: string }, private dialog: MatDialog,
    private boardService: BoardService, private toast: JDToastrService) { }

  ngOnInit() {
    this.lane = this.data.lane;
  }

  close() {
    this.dialog.closeAll();
  }

  compareFn(v1: any, v2: any) {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }

  delete() {
    this.boardService.removeBoardLane(this.data.projectKey, this.lane).subscribe(resp => {
      this.toast.success('Lane removed', this.lane.name);
      this.dialog.closeAll();
    });
  }

  save() {
    if (this.lane.id) {

    } else {
      this.lane.statuses = [];
    }
    this.boardService.saveBoardLane(this.data.projectKey, this.lane).subscribe(resp => {
      this.toast.success('Lane created', this.lane.name);
      this.dialog.closeAll();
    });
  }

}
