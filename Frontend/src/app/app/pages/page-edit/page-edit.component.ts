import { JDToastrService } from "../../toastr.service";
import { ActivatedRoute, Router } from "@angular/router";
import { Component, OnInit, NgZone, ViewChild, ElementRef, AfterViewInit } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { ProjectService } from "../../project-manage/project.service";
import { HttpService } from "../../http.service";
import { Page } from "../page";
import { PageService } from "../pages.service";
declare var editormd;
@Component({
  selector: "app-page-edit",
  templateUrl: "./page-edit.component.html",
  styleUrls: ["./page-edit.component.css"]
})
export class PageEditComponent implements OnInit, AfterViewInit {
  projectKey: string;
  wikiKey: number;
  wiki: Page;
  loading = true;
  @ViewChild('wikiTitle', { read: ElementRef, static: false }) wikiTitle: ElementRef;
  attachments = [];
  breadcrumb = [];
  editorMdInstance;
  constructor(
    private wikiService: PageService,
    public route: ActivatedRoute,
    private router: Router,
    private zone: NgZone,
    private toastr: JDToastrService,
    private projectService: ProjectService,
    private titleservice: Title
  ) {
    window['attachFromEditorFn'] = {
      zone: this.zone,
      componentFn: (name, success, failure) => this.attachFromEditor(name, success, failure),
      component: this,
    };
  }

  ngOnInit() {
    this.route.paramMap.subscribe(p => {
      this.projectKey = this.route.snapshot.parent.parent.paramMap.get(
        "projectKey"
      );
      this.wikiKey = +this.route.snapshot.paramMap.get("wikiID");
      var parent = +this.route.snapshot.queryParamMap.get("p");
      if (this.wikiKey > 0) {
        this.wikiService
          .getWiki(this.projectKey, this.wikiKey, "edit")
          .subscribe(resp => {
            this.wiki = resp;
            this.titleservice.setTitle(
              this.titleservice.getTitle() + " > " + this.wiki.title
            );
            this.getAttachments();
            if (this.wiki.parent)
              this.generateBreadbrumbs(this.wiki.parent);
            this.breadcrumb.reverse();
            setTimeout(() => {
              this.initEditor()
            }, 1000);
            //this.loading = false;
          });
      } else {
        this.wiki = new Page();
        this.wiki.id = 0;
        this.wiki.title = "";
        this.wiki.content = "";
        this.wiki.editable = true;
        if (parent) {
          this.wiki.parent = new Page();
          this.wiki.parent.id = parent;
          this.wikiService
            .getWiki(this.projectKey, parent, "view")
            .subscribe(resp => {
              this.breadcrumb.push(resp);
              if (resp.parent)
                this.generateBreadbrumbs(resp.parent);
              this.breadcrumb.reverse();
            });
        }
        setTimeout(() => {
          this.initEditor()
        }, 1000);
      }
    });
    this.getProjectMembers();
    this.wikiService.attachmentNotification.subscribe(resp => {
      if (resp.pageKey === this.wiki.id) {
        this.toastr.info("Attachment: " + resp.file, resp.status);
        if (resp.status === "Done") {
          this.getAttachments();
        }
      }
    });
  }

  clearParent() {
    this.breadcrumb = [];
    this.wiki.parent = undefined;
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.initEditor()
    }, 1000);
  }

  initEditor() {
    if (this.editorMdInstance && this.editorMdInstance[0])
      this.editorMdInstance[0].innerText = '';
    if (!this.wiki) return;
    this.editorMdInstance = editormd("edit-editormd", {
      width: "100%",
      height: '70vh',
      syncScrolling: "single",
      path: "/assets/editormd/lib/",
      htmlDecode: "style,script,iframe,sub,sup|*",
      toolbarIcons: function () {
        return ["undo", "redo", "|",
          "bold", "del", "italic", "quote", "ucwords", "uppercase", "lowercase", "|",
          "h1", "h2", "h3", "h4", "h5", "h6", "|",
          "list-ul", "list-ol", "hr", "|",
          "link", "reference-link", "image", "code", "preformatted-text", "code-block", "table", "html-entities", "|",
          "goto-line", "watch", "preview", "clear", "search"];
      },
    });
    this.loading = false;
  }


  generateBreadbrumbs(f: Page) {
    this.breadcrumb.push(f);
    if (f.parent) {
      this.generateBreadbrumbs(f.parent);
    }
  }

  attachFromEditor(file, successFn, failureFn) {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);
    const fileType = file['type'];
    const validImageTypes = ['image/jpg', 'image/jpeg', 'image/png'];
    if (!validImageTypes.includes(fileType)) {
      this.toastr.error("Image not supported", "only images allowed");
      failureFn("Image not supported");
    } else {
      this.wikiService.upload(this.projectKey, this.wiki.id, formData).subscribe(resp => {
        successFn(HttpService.getBaseURL() + resp.location);
        this.getAttachments();
      }, err => failureFn("Error uploading file"));
    }
  }

  attachFile(event) {
    event.preventDefault();
    this.attach(event.target.files);
  }

  attach(fileList) {
    this.wikiService.attach(
      this.projectKey,
      this.wiki.id,
      fileList
    );
  }

  getAttachments() {
    this.wikiService
      .getAttachments(this.projectKey, this.wiki.id)
      .subscribe(attachments => {
        this.attachments = this.sorted(attachments);
        window['attachments'] = this.attachments;
        window['attachments_images'] = [];
        this.attachments.forEach(a => {
          if (a.previewable && a.thumbnail) {
            window['attachments_images'].push({
              title: a.originalName,
              value: HttpService.getBaseURL() + a.location
            });
          }
        });
      });
  }

  getProjectMembers() {
    this.projectService.getProjectMembers(this.projectKey).subscribe(resp => {
      window['members'] = resp;
    });
  }

  sorted(data) {
    return (data || []).sort((a: any, b: any) => (a.id < b.id ? -1 : 1));
  }

  save(content: string) {
    this.loading = true;
    this.wiki.title = this.wikiTitle.nativeElement.innerText;
    if (this.wiki.title.length <= 0) {
      this.toastr.warn('Title is required', 'Validation');
      this.loading = false;
    } else if (content.length <= 0) {
      this.toastr.warn('Content is required', 'Validation');
      this.loading = false;
    } else {
      this.wiki.content = content;//this.editorMdInstance.getMarkdown();
      this.wikiService.saveWiki(this.projectKey, this.wiki).subscribe(resp => {
        this.wikiService.wikiChange.next(true);
        this.loading = false;
        this.router.navigate(["project", this.projectKey, "page", resp.id]);
      });
    }
  }

  cancel() {
    this.loading = true;
    if (this.wiki.id > 0)
      this.router.navigate(["project", this.projectKey, "page", this.wiki.id]);
    else if (this.wiki.parent)
      this.router.navigate(["project", this.projectKey, "page", this.wiki.parent.id]);
    else
      this.router.navigate(['../../'], { relativeTo: this.route });
  }

  delete() {
    const parent = this.wiki.parent;
    this.wikiService.removeWiki(this.projectKey, this.wiki).subscribe(resp => {
      this.toastr.success("Page", "Page deleted");
      if (parent)
        this.router.navigate([
          "/project",
          this.projectKey,
          "page",
          parent.id
        ]);
      else
        this.router.navigate([
          "/project",
          this.projectKey,
          "page"
        ]);
    });
  }
}
