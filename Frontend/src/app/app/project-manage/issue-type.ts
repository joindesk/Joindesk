import { Project } from './project';
import { Workflow } from './workflow';
export class IssueType {

    public id: number; public name: string; public icon_url: string;
    public editable: boolean; public project: Project;
    public createdDate: Date; public workflow: Workflow;
    public active: boolean; public newWorkflow = false;

    constructor(public edit = false) {
    }
}
