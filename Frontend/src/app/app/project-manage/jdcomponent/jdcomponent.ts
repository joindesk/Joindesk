import { Project } from '../project';
import { IssueType } from '../issue-type';

export class JDComponent {
    id: number;
    public name: string;
    public project: Project;
    public edit: boolean;
}
