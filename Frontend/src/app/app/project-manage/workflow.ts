import { WorkflowTransition } from './workflow-transition';
import { WorkflowStep } from './workflow-step';
import { Project } from './project';
export class Workflow {

    public id: number; public name: string; public description: string;
    public editable: boolean; public project: Project;
    public createdDate: Date; public workflowSteps: WorkflowStep[];
    public defaultStep: WorkflowStep;
    public workflowTransitions: WorkflowTransition[];
    public workflowStepTransitions: WorkflowStepTransition[];
    constructor() {
    }
}

export class WorkflowStepTransition {
    public step: WorkflowStep;
    public workflowTransitions: WorkflowTransition[];
}
