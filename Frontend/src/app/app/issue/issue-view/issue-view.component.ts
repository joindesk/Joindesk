import { MatDialog, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { JDToastrService } from './../../toastr.service';
import { Issue, Comment, Attachment, Relationship, IssueRelationship, WorkLog, Task, IssueOtherRelationship } from './../issue';
import { Component, OnInit, NgZone, Inject, TemplateRef, ViewChild, OnDestroy, Input } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { IssueService } from '../issue.service';

import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, Title } from '@angular/platform-browser';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { ProjectService } from '../../project-manage/project.service';
import { IssueTransitionDialogComponent } from '../issue-transition-dialog/issue-transition-dialog.component';
import { MyaccountService } from '../../myaccount/myaccountservice.service';
import { NzModalService, NzModalRef, NzNotificationService, NzNotificationDataFilled } from 'ng-zorro-antd';
import { WorkflowPreviewComponent } from '../../project-manage/project-manage-workflow/workflow-preview/workflow-preview.component';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { WorklogNzDialogComponent } from '../worklog-dialog/worklog-dialog.component';
import { HttpService } from '../../http.service';
import { IssueType } from '../../project-manage/issue-type';
import { IssueTypeService } from '../../project-manage/issue-type.service';
import { Location } from '@angular/common';
@Component({
  selector: 'app-issue-view',
  templateUrl: './issue-view.component.html',
  styleUrls: ['./issue-view.component.css']
})
export class IssueViewComponent implements OnInit, OnDestroy {
  previewImageSrc = undefined;
  previewImageInnerWidth = (window.innerWidth / 3) * 2;
  previewImageInnerHeight = (window.innerHeight / 3) * 2;
  showPreviewImageLoading = true;
  sortDir = 'asc';
  currentTabIndex = 1;
  isModal = false;
  issueError = false;
  issueLoading = false;
  commentsLoading = false;
  tasksLoading = false;
  historyLoading = false;
  workLogLoading = false;
  tplModal: NzModalRef;
  workflowLoading = false;
  currentWorkflow = undefined;
  showBranchModal = false;
  showCommitsModal = false;
  branches = [];
  commits = [];
  showIcon = [];
  taskInput = '';
  projectKey: string;
  issueKey: number;
  issue: Issue;
  comment = undefined;
  watching = false;
  watchloading = false;
  comments: Comment[];
  tasks: Task[];
  relationshipTypes = [];
  issueLinkOptionsSearchChange$ = new BehaviorSubject('');
  issueLinkOptions = [];
  issueLinkOptionsLoading = false;
  showAddTask = false;
  showAddLink = false;
  showAddwebLink = false;
  webLink = new IssueOtherRelationship;
  newIssueRelationship = {
    relationship: new Relationship,
    dest: ''
  };
  modules: {};
  fieldLoading = {
    'summary': false,
    'priority': false,
    'IssueType': false,
    'reporter': false,
    'assignee': false,
    'description': false,
    'DueDate': false,
    'StartDate': false,
    'EndDate': false,
    'timeOriginalEstimate': false,
    'component': false,
    'versions': false,
    'label': false,
    'transition': false
  }
  updateNotification: NzNotificationDataFilled;
  private notificationSubscription: Subscription = new Subscription();
  public issueChange = new Subject();
  public descMention = new Subject();
  @Input() issueKeyFromModal?: string;
  @ViewChild('updatedNotificationTemplate', { static: false }) updatedNotificationTemplate: TemplateRef<{}>;
  constructor(private router: Router, private route: ActivatedRoute, private issueService: IssueService, private modalService: NzModalService,
    private sanitizer: DomSanitizer, private toastr: JDToastrService, private projectService: ProjectService,
    public dialog: MatDialog, private titleservice: Title, private zone: NgZone, private mya: MyaccountService,
    private notification: NzNotificationService, public issueTypeService: IssueTypeService, private location: Location,
    private myaccountService: MyaccountService) {
    window['openIssueFn'] = {
      zone: this.zone,
      componentFn: (v1, v2) => this.openIssue(v1, v2),
      component: this,
    };
    window['downloadFileFn'] = {
      zone: this.zone,
      componentFn: (id, originalName) => this.downloadFile(id, originalName),
      component: this,
    };
    window['previewImageFn'] = {
      zone: this.zone,
      componentFn: (name) => this.previewImage(name),
      component: this,
    };
    window['attachFromEditorFn'] = {
      zone: this.zone,
      componentFn: (name, success, failure) => this.attachFromEditor(name, success, failure),
      component: this,
    };
  }

  ngOnInit() {
    if (!this.issueKeyFromModal) {
      this.route.params.subscribe(params => {
        const issueKeyPair = params['issueKeyPair'];
        this.projectKey = issueKeyPair.substr(0, issueKeyPair.indexOf('-'));
        this.issueKey = +issueKeyPair.substr(issueKeyPair.indexOf('-') + 1, issueKeyPair.length);
        this.getwithLoading();
      });
    } else {
      this.isModal = true;
      const issueKeyPair = this.issueKeyFromModal;
      this.projectKey = issueKeyPair.substr(0, issueKeyPair.indexOf('-'));
      this.issueKey = +issueKeyPair.substr(issueKeyPair.indexOf('-') + 1, issueKeyPair.length);
      this.getwithLoading();
    }
    this.issueService.attachmentNotification.subscribe(resp => {
      if (resp.issueKey === this.issue.key) {
        this.toastr.infoMessage('Attachment ' + resp.file + ' ' + resp.status);
        if (resp.status === 'Done') {
          this.getAttachments();
        }
      }
    });
    this.notificationSubscription.add(
      this.issueService.issueNotification.subscribe(resp => {
        resp = JSON.parse(resp);
        if (resp["project"] == this.projectKey && resp["issue"] == this.issue.keyPair
          && (window && window.location && window.location.pathname == "/issue/" + this.issue.keyPair)) {
          if (this.issue.updated != resp["updated"])
            this.updateNotification = this.notification.template(this.updatedNotificationTemplate, { nzDuration: 0, nzKey: resp["issue"] });
          else if (this.updateNotification)
            this.notification.remove(this.updateNotification.messageId);
        }
      }));
    this.getProjectMembers();
    this.issueService.getRelationshipTypes().subscribe(r => {
      r.forEach(rr => {
        let rela = new Relationship;
        rela.id = rr.id;
        rela.name = rr.inwardDesc;
        rela.type = 'IN';
        this.relationshipTypes.push(rela);
        rela = new Relationship;
        rela.id = rr.id;
        rela.name = rr.outwardDesc;
        rela.type = 'OUT';
        this.relationshipTypes.push(rela);
      });
    });
    this.issueTypeService.getIssueTypes(this.projectKey);
  }

  ngOnDestroy() {
    this.notificationSubscription.unsubscribe();
    if (this.updateNotification)
      this.notification.remove(this.updateNotification.messageId);
  }

  getProjectMembers() {
    this.projectService.getProjectMembers(this.projectKey).subscribe(resp => {
      window['members'] = resp;
      this.projectService.projectMembers[this.projectKey] = resp;
    });
  }

  openIssue(projectKey: string, issueKey: number) {
    this.router.navigate(['issue', projectKey + '-' + issueKey]);
  }

  getwithLoading() {
    this.issueLoading = true;
    this.get();
  }

  get() {
    this.issueService.getIssue(this.projectKey, this.issueKey).subscribe(resp => {
      this.issue = resp;
      this.issue.customFields = (this.issue.customFields || []).sort((a: any, b: any) => a.id < b.id ? -1 : 1);
      this.titleservice.setTitle(this.titleservice.getTitle() + ' > [' + this.issue.keyPair + '] ' + this.issue.summary);
      this.getAdditional();
      this.issueChange.next(this.issue.description);
      this.issueLoading = false;
      Object.entries(this.fieldLoading).forEach(
        ([key, value]) =>
          this.fieldLoading[key] = false
      );
      if (this.updateNotification)
        this.notification.remove(this.updateNotification.messageId);
      setTimeout(() => {
        this.location.replaceState('issue/' + this.issue.keyPair);
      }, 300);
    }, error => this.issueError = true);
  }

  getAdditional() {
    this.getComments();
    this.getTasks();
    this.getAttachments();
    //this.getHistory();
    this.getIssueRelationships();
    this.getIssueOtherRelationships();
    this.getIssueMentions();
    //this.getWorkLogs();
    this.checkWatching();
  }

  getMinimal() {
    this.issueService.getIssue(this.projectKey, this.issueKey).subscribe(resp => {
      this.issue = resp;
      this.issue.customFields = (this.issue.customFields || []).sort((a: any, b: any) => a.id < b.id ? -1 : 1);
      this.titleservice.setTitle(this.titleservice.getTitle() + ' > [' + this.issue.keyPair + '] ' + this.issue.summary);
      this.issueChange.next(this.issue.description);
      this.issueLoading = false;
      Object.entries(this.fieldLoading).forEach(
        ([key, value]) =>
          this.fieldLoading[key] = false
      );
      if (this.updateNotification)
        this.notification.remove(this.updateNotification.messageId);
      setTimeout(() => {
        this.location.replaceState('issue/' + this.issue.keyPair);
      }, 300);
    }, error => this.issueError = true);
  }

  getIssue() {
    this.issueService.getIssue(this.projectKey, this.issueKey).subscribe(resp => {
      this.issue.timeSpent = resp.timeSpent;
      this.issue.timeSpentString = resp.timeSpentString;
      this.issue.timeSpent = resp.timeSpent;
      this.issue.estimateString = resp.estimateString;
    });
  }

  getHistory() {
    this.historyLoading = true;
    this.issueService.getHistory(this.projectKey, this.issueKey).subscribe(history => {
      this.issue.history = this.customSorted(history);
      this.historyLoading = false;
    });
  }

  getBranches() {
    this.issueService.getBranches(this.projectKey, this.issueKey).subscribe(data => {
      this.branches = data;
      this.showBranchModal = true;
    });
  }

  getCommits() {
    this.issueService.getCommits(this.projectKey, this.issueKey).subscribe(data => {
      this.commits = data;
      this.showCommitsModal = true;
    });
  }

  getComments() {
    this.commentsLoading = true;
    this.issueService.getComments(this.projectKey, this.issueKey).subscribe(c => {
      this.sortComments(c);
      if (this.route.snapshot.queryParams['c']) {
        setTimeout(() => {
          const itemToScrollTo = document.getElementById('comment-' + this.route.snapshot.queryParams['c']);
          if (itemToScrollTo) {
            itemToScrollTo.scrollIntoView(true);
            itemToScrollTo.classList.add("commentHighlight");
          }
        }, 500);
      }
    });
  }

  checkWatching() {
    this.watching = false;
    this.issue.watchers.forEach(w => {
      if (this.mya.user && w.watcher.userName === this.mya.user.userName) {
        this.watching = true;
      }
    });
  }

  getTasks() {
    this.tasksLoading = true;
    this.showAddTask = false;
    this.issueService.getTasks(this.projectKey, this.issueKey).subscribe(c => {
      this.tasks = (c || []).sort((a: Task, b: Task) => a.id < b.id ? -1 : 1);
      this.tasksLoading = false;
    }, err => {
      this.tasks = [];
    });
  }

  addTask() {
    this.tasksLoading = true;
    let task = new Task();
    task.summary = this.taskInput;
    this.issueService.saveTask(this.projectKey, this.issue, task).subscribe(t => {
      this.toastr.success('Task', 'Saved');
      this.getTasks();
      this.taskInput = '';
    });
  }

  completeTask(task: Task, complete: boolean) {
    task.completed = complete;
    task.completedDate = complete ? new Date() : undefined;
    this.issueService.completeTask(this.projectKey, this.issue, task).subscribe(resp => {
      this.getTasks();
      if (complete)
        this.toastr.successMessage('Task marked as complete');
      else
        this.toastr.infoMessage('Task marked as pending');
    });
  }

  reOrderTask(event: CdkDragDrop<string[]>): void {
    console.log(event);
    //const taskID = +event.item.element.nativeElement.dataset['task'];
    moveItemInArray(this.tasks, event.previousIndex, event.currentIndex);
    let tasksCopy = [];
    for (let index = 0; index < this.tasks.length; index++) {
      this.tasks[index].taskOrder = index;
      tasksCopy.push({ id: this.tasks[index].id, taskOrder: index });
    }
    this.issueService.reorderTasks(this.projectKey, this.issue, tasksCopy).subscribe(resp => {
      //do nothing
    });
  }

  deleteTask(task: Task) {
    this.tasksLoading = true;
    this.toastr.confirm('').then(result => {
      if (result.value) {
        this.issueService.deleteTask(this.projectKey, this.issue, task).subscribe(resp => {
          this.toastr.successMessage('Task deleted');
          this.getTasks();
        });
      }
    });
  }

  getAttachments() {
    this.issueService.getAttachments(this.projectKey, this.issueKey).subscribe(attachments => {
      this.issue.attachments = this.sorted(attachments);
      window['attachments'] = this.issue.attachments;
      window['attachments_images'] = [];
      this.issue.attachments.forEach(a => {
        if (a.previewable && a.thumbnail) {
          window['attachments_images'].push({
            title: a.originalName,
            value: HttpService.getBaseURL() + a.location
          });
        }
      });
    });
  }

  getIssueRelationships() {
    this.showAddLink = false;
    this.issueService.getIssueRelationships(this.projectKey, this.issueKey)
      .subscribe(ir => this.issue.issueRelationships = this.sorted(ir));
  }

  getIssueOtherRelationships() {
    this.showAddwebLink = false;
    this.issueService.getIssueOtherRelationships(this.projectKey, this.issueKey)
      .subscribe(ir => this.issue.issueOtherRelationships = this.sorted(ir));
  }

  showAddwebLinkModal() {
    this.showAddwebLink = true;
    this.webLink = new IssueOtherRelationship;
  }

  addWebLink() {
    this.issueService.addIssueOtherRelationship(this.projectKey, this.issue, this.webLink)
      .subscribe(ir => {
        this.showAddwebLink = false;
        this.toastr.successMessage('Added new Web Link');
        this.getIssueOtherRelationships();
      });
  }

  removeWebLink(link: IssueOtherRelationship) {
    this.toastr.confirm('').then(result => {
      if (result.value) {
        this.issueService.deleteIssueOtherRelationship(this.projectKey, this.issue, link)
          .subscribe(ir => {
            this.showAddwebLink = false;
            this.toastr.successMessage('Web Link Removed');
            this.getIssueOtherRelationships();
          });
      }
    });
  }

  getIssueMentions() {
    this.issueService.getIssueMentions(this.projectKey, this.issueKey)
      .subscribe(ir => this.issue.issueMentions = this.sorted(ir));
  }

  getWorkLogs() {
    this.workLogLoading = true;
    if (this.issue.project.timeTracking.enabled) {
      this.issueService.getWorkLogs(this.projectKey, this.issueKey).subscribe(ir => this.issue.workLogs = this.customSorted(ir));
      this.workLogLoading = false;
    }
  }

  getTaskPercentage() {
    if (this.tasks.length <= 0) return 0;
    const a = this.tasks.filter(t => t.completed).length;
    const b = this.tasks.length;
    return Math.round(100 * Math.abs(a / b));
  }

  changeSortDirection(dir: string) {
    this.sortDir = dir;
    this.tabChange(this.currentTabIndex);
  }

  tabChange(index) {
    this.currentTabIndex = index;
    if (index === 'History') {
      this.getHistory();
    } else if (index === 'Comments') {
      this.getComments();
    } else if (index === 'Work Log') {
      this.getWorkLogs();
    }
  }

  sorted(data) {
    return (data || []).sort((a: any, b: any) => a.id < b.id ? -1 : 1);
  }

  customSorted(data) {
    if (this.sortDir == 'asc')
      return (data || []).sort((a: any, b: any) => a.id < b.id ? -1 : 1);
    else
      return (data || []).sort((a: any, b: any) => a.id > b.id ? -1 : 1);
  }

  logWork() {

  }

  onIssueLinkSearch(value: string): void {
    this.issueLinkOptionsLoading = true;
    this.issueLinkOptionsSearchChange$.next(value);
    this.issueService.searchIssues(this.projectKey, value).subscribe(resp => {
      this.issueLinkOptions = resp;
      this.issueLinkOptionsLoading = false;
    });
  }

  link() {
    const issueRelationship = new IssueRelationship;
    issueRelationship.type = this.newIssueRelationship.relationship;
    if (!issueRelationship.type.id) {
      this.toastr.error('Type', 'cannot be empty');
      return;
    }
    if (this.newIssueRelationship.relationship.type === 'OUT') {
      issueRelationship.fromIssuePair = this.projectKey + '-' + this.issue.key;
      issueRelationship.toIssuePair = this.newIssueRelationship.dest;
    } else {
      issueRelationship.toIssuePair = this.projectKey + '-' + this.issue.key;
      issueRelationship.fromIssuePair = this.newIssueRelationship.dest;
    }
    this.issueService.addIssueRelationship(this.projectKey, this.issue, issueRelationship).subscribe(resp => {
      this.toastr.success('LINKED', 'LINK ADDED');
      this.getIssueRelationships();
      this.newIssueRelationship = {
        relationship: new Relationship,
        dest: ''
      };
    });
  }

  unlink(ir: IssueRelationship) {
    this.toastr.confirm('').then(result => {
      if (result.value) {
        this.issueService.deleteIssueRelationship(this.projectKey, this.issue, ir).subscribe(resp => {
          this.toastr.success('LINK', 'REMOVED');
          this.getIssueRelationships();
        });
      }
    });
  }

  delete() {
    this.toastr.confirm('Deletion is irreversible').then((result) => {
      if (result.value) {
        this.issueService.delete(this.projectKey, this.issue).subscribe(resp => {
          this.toastr.success('Deletion', 'Completed');
          this.modalService.closeAll();
          this.router.navigate(['project', this.projectKey]);
        });
      }
    });
  }

  createComment() {
    this.comment = new Comment;
    this.comment.comment = '';
  }

  commentChange(c) {
    console.log(c);
  }

  saveComment() {
    this.getComments();
  }

  sortComments(comments) {
    if (this.sortDir == 'asc')
      this.comments = (comments || []).sort((a: Comment, b: Comment) => a.id < b.id ? -1 : 1);
    else
      this.comments = (comments || []).sort((a: Comment, b: Comment) => a.id > b.id ? -1 : 1);
    this.commentsLoading = false;
  }

  inlineItemChange(field) {
    this.fieldLoading[field] = true;
  }

  saveDesc(data) {
    if (data.prevUpdate == this.issue.updated) {
      this.issue.updated = data.lastUpdated;
      if (data.issue) {
        this.issue.summary = data.issue.summary;
        this.issue.description = data.issue.description;
        this.issue.renderedDescription = data.issue.renderedDescription;
        this.issue.currentStep = data.issue.currentStep;
        this.issue.resolution = data.issue.resolution;
        this.issue.assignee = data.issue.assignee;
        this.issue.reporter = data.issue.reporter;
        this.issue.priority = data.issue.priority;
        this.issue.versions = data.issue.versions;
        this.issue.components = data.issue.components;
        this.issue.labels = data.issue.labels;
        this.issue.startDate = data.issue.startDate;
        this.issue.endDate = data.issue.endDate;
        this.issue.dueDate = data.issue.dueDate;
        this.issue.issueType = data.issue.issueType;
        this.issue.timeSpentString = data.issue.timeSpentString;
      }
      if (this.updateNotification)
        this.notification.remove(this.updateNotification.messageId);
    } else
      this.getMinimal();
  }

  saveEditable(data) {
    this.fieldLoading[data.field] = false;
    if (data.result.success) {
      if (data.result.prevUpdate == this.issue.updated) {
        this.issue.updated = data.result.lastUpdated;
        if (data.result.issue) {
          this.issue.summary = data.result.issue.summary;
          this.issue.description = data.result.issue.description;
          this.issue.renderedDescription = data.result.issue.renderedDescription;
          this.issue.currentStep = data.result.issue.currentStep;
          this.issue.resolution = data.result.issue.resolution;
          this.issue.assignee = data.result.issue.assignee;
          this.issue.reporter = data.result.issue.reporter;
          this.issue.priority = data.result.issue.priority;
          this.issue.versions = data.result.issue.versions;
          this.issue.components = data.result.issue.components;
          this.issue.labels = data.result.issue.labels;
          this.issue.startDate = data.result.issue.startDate;
          this.issue.endDate = data.result.issue.endDate;
          this.issue.dueDate = data.result.issue.dueDate;
          this.issue.issueType = data.result.issue.issueType;
          this.issue.timeSpentString = data.result.issue.timeSpentString;
        }
        if (this.updateNotification)
          this.notification.remove(this.updateNotification.messageId);
      } else
        this.getMinimal();
    }
  }

  transition(id: number) {
    this.fieldLoading.transition = true;
    this.issueService.transition(this.projectKey, this.issue, [], id).subscribe(resp => {
      console.log(resp);
      this.issue.possibleTransitions = resp.possibleTransitions;
      this.issue.currentStep = resp.currentStep;
      this.issue.resolution = resp.resolution;
      this.issue.assignee = resp.assignee;
      this.issue.priority = resp.priority;
      if (resp.prevUpdated == this.issue.updated) {
        this.issue.updated = resp.updated;
        if (this.updateNotification)
          this.notification.remove(this.updateNotification.messageId);
        this.fieldLoading.transition = false;
      } else
        this.getMinimal();
    }, error => {
      error.tID = id;
      if (error.error.fields)
        this.transitionDialog(error);
      this.fieldLoading.transition = false;
    });
  }

  transitionDialog(data: any) {
    const dialogRef = this.dialog.open(IssueTransitionDialogComponent, {
      data: data
    });
    dialogRef.afterClosed().subscribe(data => {
      if (data && data.fields) {
        this.issueService.transition(this.projectKey, this.issue, data.fields, data.tID).subscribe(resp => {
          this.getMinimal();
        }, error => {
          error.tID = data.tID;
          if (error.error.fields)
            this.transitionDialog(error);
        });
      }
    });
  }

  isOutward(a: IssueRelationship) {
    return a.fromIssue.key === this.issue.key && a.fromIssue.project.key === this.issue.project.key;
  }

  isInward(a: IssueRelationship) {
    return a.toIssue.key === this.issue.key && a.toIssue.project.key === this.issue.project.key;
  }

  detach(a: Attachment) {
    this.toastr.confirm('').then((result) => {
      if (result.value) {
        this.issueService.deleteAttachment(this.projectKey, this.issue, a).subscribe(resp => {
          this.toastr.successMessage('Attachment Deleted');
          this.getAttachments();
        });
      }
    });

  }

  downloadFile(id, originalName) {
    this.issueService.getAttachment(this.projectKey, this.issue.key, id).subscribe((res: Blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(res);
      a.download = originalName;
      a.click();
    }, err => {
      alert("Error, attachment not found");
    });
  }

  getFileLink(a: Attachment) {
    if (a.previewable) {
      this.copy("![" + a.originalName + "](" +
        location.origin + HttpService.getBaseURL() + '/issue/' + this.projectKey + '/'
        + this.issueKey + '/attachment/' + a.id + '/)', "Embed code copied to clipboard");
    } else {
      this.copy("[" + a.originalName + "](" +
        location.origin + HttpService.getBaseURL() + '/issue/' + this.projectKey + '/'
        + this.issueKey + '/attachment/' + a.id + '/)', "Embed code copied to clipboard");
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

  preview(a: Attachment) {
    //this.downloadFile(a.id, a.originalName); return;
    if (a.previewable && a.thumbnail) {
      this.previewImage(a.name);
    } else if (a.previewable && !a.thumbnail) {
      this.previewFile(a);
    } else {
      this.downloadFile(a.id, a.originalName);
    }
  }

  previewFile(a: Attachment) {
    window.open(this.previewPath(a.name), '_blank');
  }

  previewPath(a: string) {
    return HttpService.getBaseURL() + '/issue/' + this.projectKey + '/' + this.issueKey + '/attachment/preview/' + a + '/';
  }

  previewImage(name) {
    this.previewImageSrc = this.previewPath(name);
    this.previewImageInnerWidth = (window.innerWidth / 3) * 2;
    this.previewImageInnerHeight = (window.innerHeight / 3) * 2;
    // this.dialog.open(AppIssueViewImagePreviewComponent, {
    //   data: this.previewPath(name)
    // });
  }

  dropHandler(event: DragEvent) {
    // console.log(event);
    // console.log(event.dataTransfer.files);
    event.preventDefault();
    if (this.issue && this.issue.permissions.attach) {
      this.issueService.attach(this.issue, event.dataTransfer.files);
    } else {
      this.toastr.error('Attachment', 'Access denied');
    }
  }

  attachFromEditor(file, successFn, failureFn) {
    //console.log(file);
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);
    const fileType = file['type'];
    const validImageTypes = ['image/jpg', 'image/jpeg', 'image/png'];
    if (!validImageTypes.includes(fileType)) {
      this.toastr.error("Image not supported", "only images allowed");
      failureFn("Image not supported");
    } else {
      this.issueService.upload(this.projectKey, this.issueKey + "", formData).subscribe(resp => {
        successFn(HttpService.getBaseURL() + resp.location);
      }, err => failureFn("Error uploading file"));
    }
  }

  attach(event) {
    const fileList: FileList = event.target.files;
    event.preventDefault();
    if (this.issue && this.issue.permissions.attach) {
      this.issueService.attach(this.issue, fileList);
    } else {
      this.toastr.error('Attachment', 'Access denied');
    }
  }

  dragOverHandler(event: DragEvent) {
    event.preventDefault();
  }

  onDragEnd(event: DragEvent) {
    event.preventDefault();
  }

  createNewWorkLogDialog() {
    var wLog = new WorkLog();
    wLog.workFrom = new Date();
    this.createWorkLogDialog(wLog);
  }

  showWorkflowModal(): void {
    this.workflowLoading = true;
    this.issueService.getWorkflow(this.projectKey, this.issueKey).subscribe(resp => {
      this.workflowLoading = false;
      this.currentWorkflow = resp;
      const tplModal = this.modalService.create({
        nzTitle: 'View workflow',
        nzContent: WorkflowPreviewComponent,
        nzComponentParams: {
          w: this.currentWorkflow
        },
        nzWidth: '60vw',
        nzFooter: null,
        nzClosable: true,
        nzMaskClosable: false
      });
    });
  }

  destroyWorkflowModal(): void {
    this.workflowLoading = true;
    setTimeout(() => {
      this.workflowLoading = false;
      this.tplModal.destroy();
    }, 1000);
  }

  watch(val: number) {
    this.watchloading = true;
    if (val == 1)
      this.issueService.addWatcher(this.issue.project.key, this.issue, this.mya.user).subscribe(resp => {
        this.issue.watchers = resp;
        this.checkWatching();
        this.watchloading = false;
        this.toastr.successMessage("Now you are watching this Issue");
      });
    else
      this.issueService.removeWatcher(this.issue.project.key, this.issue, this.mya.user).subscribe(resp => {
        this.issue.watchers = resp;
        this.checkWatching();
        this.watchloading = false;
        this.toastr.infoMessage("you have stopped watching this Issue");
      });
  }

  clone() {
    this.router.navigate(['project', this.projectKey, 'create', { c: this.issueKey }]);
  }

  updateIssueType(issueType: IssueType) {
    this.toastr.confirm('This process will reset issue to start state of new type, are you sure ?').then(v => {
      if (v.value)
        this.issueService.changeType(this.projectKey, this.issue.key, issueType).subscribe(resp => {
          this.getwithLoading();
        });
    })

  }

  createWorkLogDialog(workLog: WorkLog) {
    workLog.issue = this.issue;
    const modal = this.modalService.create({
      nzTitle: 'Worklog',
      nzZIndex: 800,
      nzContent: WorklogNzDialogComponent,
      nzComponentParams: {
        workLog: workLog
      },
      nzOnOk: () => new Promise(resolve => setTimeout(resolve, 1000)),
    });

    modal.afterClose.subscribe(result => {
      this.getWorkLogs();
      this.getIssue();
    });
  }

  getWorkPercentage() {
    return (this.issue.timeSpent / this.issue.timeOriginalEstimate) * 100;
  }

  scroll(el: HTMLElement) {
    el.scrollIntoView({ behavior: 'smooth' });
  }

}

@Pipe({ name: 'safeHtml' })
export class SafeHtmlPipe implements PipeTransform {
  constructor(private sanitized: DomSanitizer) { }
  transform(value) {
    return this.sanitized.bypassSecurityTrustHtml(value);
  }
}


@Component({
  selector: 'app-issue-view-image-preview',
  template: '<div class="card card-transparent m-0"><div class="card-body text-center">' +
    '<div *ngIf="showLoading" class="loading"></div>' +
    '<img src="{{data}}" [style.max-width.px]="innerWidth" [style.max-height.px]="innerHeight" (load)="showLoading = false" /></div></div>'
})
export class AppIssueViewImagePreviewComponent {
  showLoading = true;
  innerWidth: any;
  innerHeight: any;
  constructor(@Inject(MAT_DIALOG_DATA) public data: string) {
    this.innerWidth = (window.innerWidth / 3) * 2;
    this.innerHeight = (window.innerHeight / 3) * 2;
  }
}


@Pipe({ name: 'taskFilter', pure: false })
export class TaskFilterPipe implements PipeTransform {
  transform(items: Task[]): any {
    return items.sort((a, b) => a.taskOrder - b.taskOrder);
  }
}