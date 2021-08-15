import { JDComponent } from './../project-manage/jdcomponent/jdcomponent';
import { CustomField } from './../project-manage/custom-field/custom-field';
import { Subject, Observable } from 'rxjs';
import { WorkflowTransition } from './../project-manage/workflow-transition';
import { IssueType } from './../project-manage/issue-type';
import { WorkflowStep, IssueStatus } from './../project-manage/workflow-step';
import { Workflow } from './../project-manage/workflow';
import { JDUser } from './../admin/user/user';
import { Project } from './../project-manage/project';

export class Issue {

    public id: number; public summary: string; public key: string; public description: string;
    public project: Project; public reporter: JDUser; public assignee: JDUser; public keyPair: string;
    public createdDate: Date; public currentStep: WorkflowStep; public priority: string; public watchers: any;
    public resolution: string; public issueType: IssueType; public permissions: {
        'edit', 'comment', 'delete', 'assign', 'reporter', 'link', 'attach'
    };
    public created: Date; public updated: Date; public updateField: string;
    public possibleTransitions: WorkflowTransition[]; public attachments: Attachment[]; history: History[];
    public comments: Comment[]; public renderedDescription: string; public estimateString: string;
    public dueDate: string; public timeOriginalEstimate: number; public timeSpent: number;
    public issueRelationships: IssueRelationship[]; public workLogs: WorkLog[]; public timeSpentString: string;
    public versions: Version[]; public issueMentions: IssueMentions[]; public customFields: CustomField[];
    public components: JDComponent[] = []; public possibleComponents: JDComponent[];
    public labels: Label[] = []; public startDate: string; public endDate: string;
    public branchCount: number; public commitCount: number; public issueOtherRelationships: IssueOtherRelationship[];

    constructor() { }
}

export class FileProgress {
    public file: string;
    public issueKey: string;
    public status: string;
    public attachment: Attachment;
}

export class Attachment {
    public id: number;
    public file: string;
    public name: string;
    public originalName: string;
    public location: string;
    public size: number;
    public thumbnail: string;
    public previewable: boolean;
}

export class History {
    public id: number;
    public field: string;
    public by: JDUser;
    public oldValue: string;
    public newValue: string;
    public loggedOn: Date;
}

export class Comment {
    public id: number;
    public comment: string;
    public by: JDUser;
    public createdDate: Date;
    public updatedDate: Date;
    public editable: boolean;
    public deletable: boolean;
}

export class Task {
    public id: number;
    public summary: string;
    public dueDate: Date;
    public completedDate: Date;
    public completed: boolean;
    public editable: boolean;
    public deletable: boolean;
    public taskOrder: number;
}

export class Relationship {
    public id: number;
    public name: string;
    public outwardDesc: string;
    public inwardDesc: string;
    public type: string;
}

export class IssueRelationship {
    public id: number;
    public fromIssue: Issue;
    public toIssue: Issue;
    public type: Relationship;
    public fromIssuePair: string;
    public toIssuePair: string;
}

export class IssueOtherRelationship {
    public id: number;
    public issue: Issue;
    public label: string;
    public link: string;
}

export class IssueMentions {
    public id: number;
    public issue: Issue;
    public link: boolean;
    public mention: boolean;
    public linkURL: string;
}

export class IssueFilter {
    public id: number;
    public name: string;
    public owner: JDUser;
    public sortBy: string;
    public sortOrder: string;
    public query: any;
    public searchQuery: string;
    public open: boolean;
    public readonly: boolean;
    public ownerId: number;
}

export class IssueFilterDTO {
    public pageSize = 10;
    public pageIndex = 0;
    public containsText: string;
    public issueType: IssueType[];
    public resolutions: any[];
    public projects: Project[];
    public priority: string[];
    public versions: Version[];
    public assignee: JDUser[];
    public reporter: JDUser[];
    public status: IssueStatus[];
    public createdBefore: Date;
    public updatedBefore: Date;
    public createdAfter: Date;
    public updatedAfter: Date;
    public dueBefore: Date;
    public dueAfter: Date;
    public possibleSorts: string[];
    public possibleIssueTypes: IssueType[];
    public possibleResolutions: Resolution[];
    public possibleProjects: Project[];
    public possiblePriorities: string[];
    public possibleMembers: JDUser[];
    public possibleStatus: IssueStatus[];
    public filter: IssueFilter;
    public filters: IssueFilterGroup[];
    public advanced = false;
    public action: string;
}

export class IssueFilterGroup {
    public field: string;
    public label: string;
    public type: string;
    public values: string[] = [];
    public expandedValues: string[] = [];
    public value: string;
    public valueTo: string;
    public fromDate: Date;
    public toDate: Date;
    public operators: string[];
    public options: IssueFilterOptions[];
}

export class IssueFilterOptions {
    public name: string;
    public value: string;
}

export class FilteredIssues {
    public total = 0;
    public filter: IssueFilter;
    public pageSize = 10;
    public pageIndex = 1;
    public issues: Issue[];
    public timezone: string;
    public type: string;
    public fromDate: string;
    public toDate: string;
}

export class WorkLog {
    public id: number;
    public workFrom: Date;
    public workMinutes: number;
    public work: string;
    public workDescription: string;
    public issue: Issue;
    public issueKey: string;
    public createdBy: string;
    public editable: boolean;
    public deletable: boolean;
}

export class Resolution {
    public id: number;
    public name: string;
    public createdBy: string;
}

export class Label {
    public id: number;
    public name: string;
}


export class Version {
    public id: number;
    public name: string;
    public key: string;
    public status: string;
    public released: boolean;
    public description: string;
    public startDate: Date;
    public releaseDate: Date;
    public totalResolved: number;
    public totalIssues: number;
    constructor(public edit = false) {
    }
}
