import { WorkflowStep } from './workflow-step';
import { Workflow } from './workflow';
export class WorkflowTransitionProperties {

    public id: number; public name: string;
    public type: string; public subType: string; public condition: string;
    public key: string; public value: string; public displayValue: string;
    public fromStep: string; public toStep: string;
    public transitionName: string;
    constructor() {
    }
}

