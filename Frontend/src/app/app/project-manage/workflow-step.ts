import { Issue } from '../issue/issue';
import { Workflow } from './workflow';
import { JDUser } from '../admin/user/user';
export class WorkflowStep {

    public id: number; public name: string; public issueStatus: IssueStatus;
    public workflow: Workflow;
    constructor() {
    }
}

export class IssueStatus {
    public id: number;
    public name: string;
    public by: JDUser;
    public issues: Issue[];
    public issueLifeCycle: string;
    public editable: boolean;
    public deletable: boolean;
}
