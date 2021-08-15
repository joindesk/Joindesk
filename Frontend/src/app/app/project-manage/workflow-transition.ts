import { WorkflowStep } from './workflow-step';
import { Workflow } from './workflow';
export class WorkflowTransition {

    public id: number; public name: string; public description: string;
    public workflow: Workflow; public initial: boolean; public fromAll: boolean;
    public fromStep: WorkflowStep; public toStep: WorkflowStep;
    constructor() {
    }
}
