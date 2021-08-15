export class JDUser {

    public id: number; public userName: string; public fullName: string; public email: string; public superAdmin: boolean;
    public timezone: string; public language: string; public createdAt: Date; public updatedAt: Date; public active: boolean;
    public editable: boolean; public slackEnabled: boolean; public slackAvailable: boolean; public locked: boolean; public pic: string;
    public emailNotification: boolean; public slackNotification: boolean; public lockReason: string;
    public apiEnabled: boolean; public apiToken: string; public qrUrl: string; public mfaEnabled: boolean; public preferredAuth: string;

    constructor(public edit = false) {
        this.apiToken = undefined;
    }
}

export class Token {
    public id: number; public token: string; public created: Date; public lastAccess: Date;
    public deviceInfo: string; public current: boolean; public device: {};

}
