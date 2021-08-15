import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { PageService } from "../pages.service";
import { PageRevision } from "../page";
declare var editormd;

@Component({
  selector: "app-page-history",
  templateUrl: "./page-history.component.html",
  styleUrls: ["./page-history.component.css"]
})
export class PageHistoryComponent implements OnInit {
  revisions: PageRevision[] = [];
  revision: PageRevision;
  projectKey: string;
  wikiKey: number;
  source = -1;
  loading = false;
  editorMdInstance: any;
  constructor(private wikiService: PageService, public route: ActivatedRoute) { }

  ngOnInit() {
    this.route.paramMap.subscribe(p => {
      this.projectKey = this.route.snapshot.parent.parent.paramMap.get(
        "projectKey"
      );
      this.wikiKey = +this.route.snapshot.paramMap.get("wikiID");
      if (this.wikiKey > 0) {
        this.wikiService
          .getRevisions(this.projectKey, this.wikiKey)
          .subscribe(resp => (this.revisions = resp));
      }
    });
  }

  viewHistory(id: number) {
    this.loading = true;
    this.revision = undefined;
    this.source = id;
    this.wikiService.getRevision(this.projectKey, this.wikiKey, id).subscribe(resp => {
      this.revision = resp;
      this.loading = false;
      setTimeout(() => {
        this.initEditor();
      }, 200);
    });
  }

  initEditor() {
    if (this.editorMdInstance && this.editorMdInstance[0])
      this.editorMdInstance[0].innerText = '';
    this.editorMdInstance = editormd.markdownToHTML("view-editormd", {
      markdown: this.revision.content,
      htmlDecode: "style,script,iframe,sub,sup,embed|*",
      toc: false,
      tocm: false,    // Using [TOCM]
      tocContainer: "#custom-toc-container", // 自定义 ToC 容器层
      //gfm             : false,
      //tocDropdown     : true,
      // markdownSourceCode : true, // 是否保留 Markdown 源码，即是否删除保存源码的 Textarea 标签
      emoji: true,
      taskList: true,
      tex: false,
      flowChart: false,
      sequenceDiagram: false,
    });
  }
}
