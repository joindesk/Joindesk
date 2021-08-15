import { Login } from "../../auth/authentication.service";
import { Project } from "../project-manage/project";

export class Page {
  public id: number;
  public title: string;
  public content: string;
  public project: Project;
  public createdLogin: Login;
  public lastUpdatedLogin: Login;
  public created: Date;
  public updated: Date;
  public editable = false;
  public deleteable = false;
  public parent: Page;
}

export class PageData {
  public pageParent: Page;
  public pageParents: Page[];
  public path: Page[];
  public pages: Page[];
  public combined = [];
}

export class PageFileProgress {
  public file: string;
  public pageKey: number;
  public status: string;
  public attachment: PageAttachment;
}

export class PageAttachment {
  public id: number;
  public file: string;
  public name: string;
  public originalName: string;
  public size: number;
  public thumbnail: string;
  public previewable: boolean;
}

export class PageRevision {
  id: number;
  version: number;
  public title: string;
  public content: string;
  public lastUpdatedLogin: Login;
}
