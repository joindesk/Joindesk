import { WebHookHeaders } from './webhook-headers';
import { Project } from './project';
export class WebHook {

    public id: number;
    public endPoint: string;
    public name: string;
    public query: string;
    public active = false;
    public allProjects = false;
    public allEvents = false;
    public requestHeaders: WebHookHeaders[] = [];
    public projects: Project[];
    public eventTypes: string[];

    constructor(public edit = false) {
    }
}
