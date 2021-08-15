import { Project } from '../project';
import { IssueType } from '../issue-type';

export class CustomField {
    id: number;
    public name: string;
    public key: string;
    public type: string;
    public value: string;
    public values: string[];
    public validation: string;
    public defaultValue: string;
    public helpText: string;
    public multiple: boolean;
    public showOnCreate: boolean;
    public required: boolean;
    public edit: boolean;
    public project: Project;
    public issueTypes: IssueType[] = [];
}
