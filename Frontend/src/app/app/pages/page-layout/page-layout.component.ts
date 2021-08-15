import { ActivatedRoute, Router, NavigationEnd } from "@angular/router";
import { Component, OnInit, PipeTransform, Pipe } from "@angular/core";
import { find } from "rxjs/operators";
import { PageService } from "../pages.service";

@Component({
  selector: "app-page-layout",
  templateUrl: "./page-layout.component.html",
  styleUrls: ["./page-layout.component.css"]
})
export class PageLayoutComponent {
  constructor(
  ) { }
}
