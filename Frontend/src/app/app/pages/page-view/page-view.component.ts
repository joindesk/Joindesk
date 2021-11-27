import { ActivatedRoute, Router } from "@angular/router";
import { Component, OnInit, NgZone, HostListener, ElementRef, ViewChild, AfterViewInit, TemplateRef } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { MatDialog } from "@angular/material/dialog";
import { JDToastrService } from "../../toastr.service";
import { NzFormatEmitEvent, NzModalRef, NzModalService } from "ng-zorro-antd";
import { Page, PageAttachment } from "../page";
import { PageService } from "../pages.service";
import { AuthenticationService } from "../../../auth/authentication.service";
import { AppIssueViewImagePreviewComponent } from "../../issue/issue-view/issue-view.component";
import { HttpService } from "../../http.service";
import { Location } from "@angular/common";
declare var editormd;
@Component({
  selector: "app-page-view",
  templateUrl: "./page-view.component.html",
  styleUrls: ["./page-view.component.css"]
})
export class PageViewComponent implements OnInit, AfterViewInit {
  @ViewChild('contentView', { read: ElementRef, static: false }) contentView: ElementRef;
  @ViewChild('searchInput', { static: false }) searchInput: ElementRef;
  activeNode: number;
  projectKey: string;
  wikiKey: number;
  wiki: Page;
  hideSideBar = false;
  hover = [];
  tree: any;
  breadcrumb = [];
  editorMdInstance: any;
  loading = false;
  showIcon = [];
  recentPages: Page[];
  canCreate: boolean;
  searchDrawerModal = false;
  public recentWikis: Page[] = [];
  public searching = false;
  public wikiResults: Page[] = undefined;
  targetFolderName = undefined;
  target: Page;
  currentAction = {};
  copyModal: NzModalRef;
  attachments = [];
  constructor(
    private wikiService: PageService,
    public route: ActivatedRoute,
    private toastr: JDToastrService,
    private router: Router,
    private zone: NgZone,
    private titleservice: Title,
    public dialog: MatDialog,
    private authService: AuthenticationService,
    private modalService: NzModalService, private location: Location
  ) {
    window['scrollFn'] = {
      zone: this.zone,
      componentFn: (id) => this.scrollTo(id),
      component: this,
    };
  }

  ngOnInit() {
    this.canCreate = this.authService.hasAuthority('WIKI_EDIT');
    this.route.paramMap.subscribe(p => {
      this.loading = true;
      this.projectKey = this.route.snapshot.parent.parent.paramMap.get(
        "projectKey"
      );
      this.wikiKey = +this.route.snapshot.paramMap.get("wikiID");
      this.activeNode = this.wikiKey;
      if (this.wikiKey > 0) {
        this.load();
      } else {
        this.getRecent();
        this.getTree();
      }
    });

    this.wikiService.attachmentNotification.subscribe(resp => {
      if (resp.pageKey === this.wiki.id) {
        this.toastr.infoMessage('Attachment ' + resp.file + ' ' + resp.status);
        if (resp.status === 'Done') {
          this.getAttachments();
        }
      }
    });
  }

  load() {
    this.loading = true;
    this.breadcrumb = [];
    this.wikiService
      .getWiki(this.projectKey, this.wikiKey, "view")
      .subscribe(resp => {
        this.wiki = resp;
        if (!this.tree)
          this.getTree();
        this.titleservice.setTitle(
          this.titleservice.getTitle() + " > " + this.wiki.title
        );
        if (this.wiki.parent)
          this.generateBreadbrumbs(this.wiki.parent);
        this.breadcrumb.reverse();
        this.location.replaceState('project/' + this.projectKey + '/page/' + this.wikiKey);
        setTimeout(() => {
          this.initEditor()
        }, 1000);
        this.getAttachments()
      });
  }

  create() {
    this.router.navigate(["../edit/0"], { queryParams: { p: this.wikiKey }, relativeTo: this.route });
  }

  getTree() {
    this.wikiService
      .getWiki(this.projectKey, this.wikiKey, "tree")
      .subscribe(resp => {
        this.tree = resp;
      });
  }

  nzEvent(event: Required<NzFormatEmitEvent>): void {
    console.log(event);
    // load child async
    if (event.eventName === 'expand') {
      this.expand(event);
    }
  }

  @HostListener("window:toggleSidebar", ['$event'])
  ontoggleSidebar(event: CustomEvent) {
    console.log(event);
    this.hideSideBar = event.detail.hide;
  }

  expand($event) {
    const node = $event.node;
    if (node && node.getChildren().length === 0 && node.isExpanded) {
      this.wikiService
        .getChildrens(this.projectKey, node.key)
        .subscribe(resp => {
          console.log(resp);
          node.addChildren(resp);
        });
    }
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
    this.editorMdInstance = editormd.markdownToHTML("view-editormd", {
      markdown: this.wiki.content,
      //htmlDecode      : true, 
      htmlDecode: "style,script,iframe,sub,sup,embed|*",
      tocContainer: "#custom-toc-container",
      tocDropdown: false,
      //gfm             : false,
      //tocDropdown     : true,
      // markdownSourceCode : true, // 是否保留 Markdown 源码，即是否删除保存源码的 Textarea 标签
      emoji: true,
      taskList: true,
      tex: false,
      flowChart: false,
      sequenceDiagram: false,
    });
    this.loading = false;
  }

  openSearch() {
    if (this.recentWikis.length <= 0)
      this.getRecent();
    this.searchDrawerModal = true;
    this.searchInput.nativeElement.focus();
  }

  getRecent() {
    this.wikiService.getRecent(this.projectKey).subscribe(resp => {
      this.recentPages = resp;
      this.recentWikis = resp;
    });
  }

  search(q: string) {
    if (q.length > 0) {
      this.wikiService
        .searchWiki(this.projectKey, q)
        .subscribe(resp => (this.wikiResults = resp));
    } else {
      this.wikiResults = [];
    }
  }

  scrollTo(hashTag: string) {
    const itemToScrollTo = document.getElementById(hashTag);
    if (itemToScrollTo) {
      let topOfElement = itemToScrollTo.offsetTop - 70;
      window.scroll({ top: topOfElement, behavior: "smooth" })
      //itemToScrollTo.scrollIntoView(true);
    }
  }

  generateBreadbrumbs(f: Page) {
    this.breadcrumb.push(f);
    if (f.parent) {
      this.generateBreadbrumbs(f.parent);
    }
  }

  navigate(f: string, id: number) {
    if (f == "folder") {
      this.router.navigate(["../folder/", id], { relativeTo: this.route });
      //this.router.navigateByUrl('../' + id);
    } else if (f == "file") {
      //this.router.navigateByUrl('../../' + id);
      this.router.navigate(["../", id], { relativeTo: this.route });
    }
  }

  openFolder($event) {
    this.activeNode = $event.node.key;
    this.wikiKey = $event.node.key;
    this.load();
    //this.router.navigateByUrl('../' + $event.node.key);
    //this.router.navigate(["../", $event.node.key], { relativeTo: this.route });
  }

  delete() {
    const parent = this.wiki.parent;
    this.toastr.confirm('Deletion is irreversible').then((result) => {
      if (result.value) {
        this.wikiService.removeWiki(this.projectKey, this.wiki).subscribe(resp => {
          this.toastr.success('Deletion', 'Completed');
          if (parent)
            this.router.navigate(["project", this.projectKey, "page", parent.id]);
          else
            this.router.navigate(['../'], { relativeTo: this.route });
        });
      }
    });
  }

  copyPageModal(f: any, type: string, target: string, tplContent: TemplateRef<{}>, tplFooter: TemplateRef<{}>) {
    this.target = undefined;
    this.currentAction = {};
    this.currentAction['target'] = target;
    this.currentAction['type'] = type;
    this.currentAction['data'] = f;
    this.createTplModal('Copy', tplContent, tplFooter);
  }

  movePageModal(f: any, type: string, target: string, tplContent: TemplateRef<{}>, tplFooter: TemplateRef<{}>) {
    this.target = undefined;
    this.currentAction = {};
    this.currentAction['target'] = target;
    this.currentAction['type'] = type;
    this.currentAction['data'] = f;
    this.createTplModal('Move', tplContent, tplFooter);
  }

  createTplModal(tplTitle: string, tplContent: TemplateRef<{}>, tplFooter: TemplateRef<{}>): void {
    this.copyModal = this.modalService.create({
      nzTitle: tplTitle,
      nzContent: tplContent,
      nzFooter: tplFooter,
      nzMaskClosable: false,
      nzClosable: false
    });
  }

  performAction() {
    let wikiFolder = new Page();
    wikiFolder.id = +this.target;
    this.loading = true;
    if (this.currentAction['type'] === "copy" && this.currentAction['target'] === "document") {
      this.wikiService
        .copyWiki(this.projectKey, this.currentAction['data'], wikiFolder)
        .subscribe(resp => {
          this.toastr.success("Copied", "");
          this.loading = false;
          this.copyModal.close();
          this.router.navigate(["../", resp.id], { relativeTo: this.route });
        });
    }
    if (this.currentAction['type'] === "move" && this.currentAction['target'] === "document") {
      this.wikiService
        .moveWiki(this.projectKey, this.currentAction['data'], wikiFolder)
        .subscribe(resp => {
          this.toastr.success("Moved", "");
          this.loading = false;
          this.copyModal.close();
          this.load();
        });
    }
  }

  closeModal() {
    this.copyModal.close();
  }

  @HostListener('window:custom-event', ['$event'])
  updateNodes(event) {
    console.log(event);
  }

  openWiki(projectKey: string, wikiID: number) {
    this.router.navigate(["project", projectKey, "wiki", wikiID]);
  }

  openIssue(projectKey: string, issueKey: number) {
    this.router.navigate(["issue", projectKey + "-" + issueKey]);
  }

  copyText() {
    let selBox = document.createElement('textarea');
    selBox.style.position = 'fixed';
    selBox.style.left = '0';
    selBox.style.top = '0';
    selBox.style.opacity = '0';
    selBox.value = window.location.href;
    document.body.appendChild(selBox);
    selBox.focus();
    selBox.select();
    document.execCommand('copy');
    document.body.removeChild(selBox);
    this.toastr.success('Copied', 'Page link copied to clipboard');
  }

  getAttachments() {
    this.wikiService
      .getAttachments(this.projectKey, this.wiki.id)
      .subscribe(attachments => {
        this.attachments = this.sorted(attachments);
      });
  }

  sorted(data) {
    return (data || []).sort((a: PageAttachment, b: PageAttachment) =>
      a.originalName < b.originalName ? -1 : 1
    );
  }

  detach(a: PageAttachment) {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.wikiService
          .deleteAttachment(this.projectKey, this.wiki.id, a)
          .subscribe(resp => {
            this.toastr.success("Attachment", "Deleted");
            this.getAttachments();
          });
      }
    });
  }

  dropHandler(event: DragEvent) {
    event.preventDefault();
    this.attach(event.dataTransfer.files);
  }

  attachFile(event) {
    event.preventDefault();
    this.attach(event.target.files);
  }

  attach(fileList: FileList) {
    this.toastr.infoMessage("Attaching " + fileList.length + " files");
    this.wikiService.attach(
      this.projectKey,
      this.wiki.id,
      fileList
    );
  }

  preview(a: PageAttachment) {
    //this.downloadFile(a.id, a.originalName);  return;
    if (a.previewable && a.thumbnail) {
      this.previewImage(a);
    } else if (a.previewable && !a.thumbnail) {
      this.previewFile(a);
    } else {
      this.downloadFile(a, a.originalName);
    }
  }

  downloadFile(id, originalName) {
    this.wikiService.getAttachment(this.projectKey, this.wiki.id, id).subscribe((res) => {
      if (res.status != 200) {
        this.toastr.error('error', 'cannot get attachment');
      } else {
        const a = document.createElement("a");
        a.href = URL.createObjectURL(res.body);
        a.download = originalName;
        a.click();
      }
    }, err => {
      alert("Error, attachment not found");
    });
  }

  previewFile(a: PageAttachment) {
    window.open(this.previewPath(a.id), "_blank");
  }

  previewPath(a: number) {
    return (
      HttpService.getBaseURL() +
      "/page/" +
      this.projectKey +
      "/" +
      this.wiki.id +
      "/attachment/" +
      a +
      "/preview/"
    );
  }

  previewImage(a: PageAttachment) {
    const dialogRef = this.dialog.open(AppIssueViewImagePreviewComponent, {
      data: this.previewPath(a.id)
    });
  }

  getFileLink(a: PageAttachment) {
    if (a.previewable) {
      this.copy("![" + a.originalName + "](" +
        location.origin + HttpService.getBaseURL() + '/page/' + this.projectKey + '/' + this.wiki.id
        + '/attachment/' + a.id + '/)', "Embed code copied to clipboard");
    } else {
      this.copy("[" + a.originalName + "](" +
        location.origin + HttpService.getBaseURL() + '/page/' + this.projectKey + '/' + this.wiki.id
        + '/attachment/' + a.id + '/)', "Embed code copied to clipboard");
    }
  }

  copy(value, message) {
    let selBox = document.createElement('textarea');
    selBox.style.position = 'fixed';
    selBox.style.left = '0';
    selBox.style.top = '0';
    selBox.style.opacity = '0';
    selBox.value = value;
    document.body.appendChild(selBox);
    selBox.focus();
    selBox.select();
    document.execCommand('copy');
    document.body.removeChild(selBox);
    this.toastr.successMessage(message);
  }

  dragOverHandler(event: DragEvent) {
    event.preventDefault();
  }

  onDragEnd(event: DragEvent) {
    event.preventDefault();
  }

}
