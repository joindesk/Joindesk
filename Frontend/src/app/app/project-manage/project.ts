import { JDUser } from './../admin/user/user';
export class Project {

    public id: number; public name: string; public key: string; public description: string;
    public created: Date; public active: boolean; public lead: JDUser;
    public editable: boolean; public timeTracking: TimeTracking; public authorities: string[];
    public slackChannel: SlackChannel; public notifyViaSlack: boolean;

    constructor(public edit = false) {
    }
}

export class TimeTracking {
    public id: number;
    public enabled: boolean;
    public hoursPerDay: number;
    public daysPerWeek: number;
    public timeFormat: number;
}

export class SlackChannel {
    public id: string;
    public name: string;
}

export class GitRepository {
    public id: number;
    public name: string;
    public repoUrl: string;
    public uuid: string;
    public active: boolean;
    public repoType: string;
    public hookEndpoint: string;
    public edit: boolean;
}

export class GitHook {
    public id: number;
    public hookName: string;
    public uuid: string;
}