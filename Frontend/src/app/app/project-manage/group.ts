import { Project } from './project';
import { JDUser }from '../admin/user/user';
export class Group {
         public id: number;
         public name: string;
         public editable: boolean;
         public project: Project;
         public authorityCodes: string[];
         public users: JDUser[];
        public allUsers: boolean;

         constructor(public edit = false) {}
       }
