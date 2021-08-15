import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { JDUser } from '../../admin/user/user';
import { JDToastrService } from '../../toastr.service';
import { Comment, Issue, Attachment } from '../issue'
import { IssueService } from '../issue.service';
import { HttpService } from '../../http.service';
declare var showdown;
@Component({
  selector: 'app-jd-editor',
  templateUrl: './jd-editor.component.html',
  styleUrls: ['./jd-editor.component.css']
})
export class JdEditorComponent implements OnInit {
  preview: SafeHtml;
  loading = false;
  actionLoading = false;
  @Input() commentCopy: Comment;
  @Input() type: string = 'new';
  @Input() field: string = 'comment';
  @Input() issue: Issue;
  @Input() user: JDUser;
  showIcon = [];
  @Input() comment: Comment;
  @Output() commentChange = new EventEmitter<Comment>();
  @Input() editable = false;
  @Input() desc: string;
  @Output() descChange = new EventEmitter<string>();
  @Input() descCopy: string;
  @Output() save = new EventEmitter<any>();
  @Output() triggerAttachment = new EventEmitter<any>();
  suggestions = window['members'];
  mode = 'write';
  @Input() editmode = 'view';
  attachments;
  imageAttachments;
  reviewHtml: SafeHtml;
  rand = Math.floor(Math.random() * 999) + 1;;
  showDownOptions = { prefixHeaderId: 'JDC-', 'strikethrough': true, 'tables': true, 'tasklists': true, 'simpleLineBreaks': true };
  constructor(private sanitizer: DomSanitizer, private elementRef: ElementRef, private issueService: IssueService,
    private toastr: JDToastrService) { }

  ngOnInit() {
    var myext = function () {
      var table = {
        type: 'output',
        regex: /<table([> ])/,
        replace: '<table class="table"$1'
      };
      var mention2 = {
        type: 'output',
        regex: /@[A-Za-z0-9.+_-]*/g,
        replace: '<b class="mention">@$1</b>'
      };
      var mention = {
        type: 'output',
        filter: function (text, converter, options) {
          const regex = /\B\@([\w\.+-]+)/gim;
          return text.replace(regex, function (match) {
            return '<b class="mention">' + match + '</b>';
          })
        }
      };
      var attachment = {
        type: 'output',
        filter: function (text, converter, options) {
          const regex = /(?<=<a.*>).+(?=<\/a>)/g;
          const result = text.replace(regex, function (m, g) {
            console.log(m);
            console.log(g);
          });
          return result;
        }
      };
      return [mention, table];
    };
    showdown.extension('myext', myext);
    this.showDownOptions['extensions'] = ['myext'];
    if (this.field == 'comment' && this.type != 'new' && this.editmode == 'view') {
      this.getComment();
    }
    if (this.field == 'desc' && this.editmode == 'view') {
      this.getDesc();
    }
  }

  getAttachments() {
    this.attachments = this.issue.attachments;
  }

  append(a: Attachment) {
    let htmlRef = this.elementRef.nativeElement.querySelector(`#commentsArea-` + this.rand);
    if (htmlRef) {
      htmlRef.focus();
      var value = this.comment.comment + "[file:" + a.originalName + ":" + a.id + "](#) ";
      this.comment.comment = value;
    }
  }

  createComment() {
    if (this.type == 'new') {
      this.comment = new Comment;
      this.comment.comment = '';
    }
  }

  change() {
    this.commentChange.emit(this.comment);
  }

  descchange() {
    this.descChange.emit(this.desc);
  }

  valueWith = (data: JDUser) => data.userName;

  getRegExp(prefix: string | string[]): RegExp {
    const prefixArray = Array.isArray(prefix) ? prefix : [prefix];
    let prefixToken = prefixArray.join('').replace(/(\$|\^)/g, '\\$1');

    if (prefixArray.length > 1) {
      prefixToken = `[${prefixToken}]`;
    }

    return new RegExp(`(\\s|^)(${prefixToken})[^\\s]*`, 'g');
  }

  renderPreView(data: string): void {
    if (data) {
      this.loading = true;
      const regex = this.getRegExp('@');
      const previewValue = data.replace(
        regex,
        match => `**${match}**`
      );
      var converter = new showdown.Converter(this.showDownOptions);
      this.preview = this.sanitizer.bypassSecurityTrustHtml(converter.makeHtml(previewValue));
      this.loading = false;
    }
  }

  getComment() {
    this.editmode = 'view';
    var converter = new showdown.Converter(this.showDownOptions);
    this.reviewHtml = this.sanitizer.bypassSecurityTrustHtml(converter.makeHtml(this.comment.comment));
  }

  getDesc() {
    this.editmode = 'view';
    var converter = new showdown.Converter(this.showDownOptions);
    this.reviewHtml = this.sanitizer.bypassSecurityTrustHtml(converter.makeHtml(this.desc));
  }

  resetComment() {
    if (this.type == 'new') {
      this.comment = undefined;
    } else {
      this.comment = Object.assign({}, this.commentCopy);
      this.getComment();
    }
  }

  resetDesc() {
    if (this.descCopy) {
      this.desc = this.descCopy;
    }
    this.getDesc();
  }

  saveComment() {
    this.commentCopy = undefined;
    this.renderPreView(this.comment.comment);
    if (this.comment.comment === '') {
      this.toastr.error('Comment', 'Comment is required');
    } else {
      this.actionLoading = true;
      this.issueService.comment(this.issue.project.key, this.issue, this.comment).subscribe(resp => {
        this.toastr.success('Comment', 'Saved');
        this.comment = undefined;
        this.actionLoading = false;
        this.save.emit();
      });
    }
  }

  saveDesc() {
    this.descCopy = undefined;
    this.renderPreView(this.desc);
    if (this.desc === '') {
      this.toastr.error('Description', 'Description is required');
    } else {
      this.actionLoading = true;
      const issue = new Issue();
      issue.key = this.issue.key;
      issue.description = this.desc;
      this.issueService.save(this.issue.project.key, issue).subscribe(resp => {
        this.toastr.success('Description', 'Saved');
        this.getDesc();
        this.actionLoading = false;
        this.save.emit(resp);
      });
    }
  }

  deleteComment() {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.actionLoading = true;
        this.issueService.deleteComment(this.issue.project.key, this.issue, this.comment).subscribe(resp => {
          this.toastr.success('Comment', 'Deleted');
          this.save.emit();
        });
      }
    });
  }

  editComment() {
    this.editmode = 'edit';
    this.commentCopy = Object.assign({}, this.comment);
    let htmlRef = this.elementRef.nativeElement.querySelector(`#commentsArea-` + this.rand);
    if (htmlRef) { htmlRef.focus(); }
  }

  editDesc() {
    this.editmode = 'edit';
    this.descCopy = this.desc;
    let htmlRef = this.elementRef.nativeElement.querySelector(`#commentsArea-` + this.rand);
    if (htmlRef) { htmlRef.focus(); }
  }

  getCommentURL(id) {
    return "https://" + window.location.host + "/issue/" + this.issue.project.key + "-" + this.issue.key + ((id > 0) ? "?c=" + id : '');
  }

  copyLink(id) {
    let selBox = document.createElement('textarea');
    selBox.style.position = 'fixed';
    selBox.style.left = '0';
    selBox.style.top = '0';
    selBox.style.opacity = '0';
    selBox.value = this.getCommentURL(id);
    document.body.appendChild(selBox);
    selBox.focus();
    selBox.select();
    document.execCommand('copy');
    document.body.removeChild(selBox);
    this.toastr.successMessage('Link copied to clipboard');
  }

}
