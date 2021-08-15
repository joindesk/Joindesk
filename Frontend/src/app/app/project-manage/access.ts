export class Access {

    public id: number; public cidr: string;
    public description: string;
    public enabled = false;
    public apiOnly = false;
    public whiteList = false;

    constructor(public edit = false) {
    }
}
