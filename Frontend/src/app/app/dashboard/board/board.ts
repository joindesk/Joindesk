import { Issue, Label } from './../../issue/issue';
import { IssueStatus } from './../../project-manage/workflow-step';
import { JDUser }from '../../admin/user/user';
export class Board {

    public id: number; public name: string;
    public lanes: Lane[];
    public query: string;
    public filter: any;
    public active = false;
    public edit = false;
}

export class Lane {
    public id: number; public name: string; public board: Board;
    public laneOrder: number; public statuses: IssueStatus[];
    possibleStatuses: IssueStatus[]; issues: Issue[];
}

export class BoardFilter {
    public member: JDUser[];
    public labels: Label[];
}