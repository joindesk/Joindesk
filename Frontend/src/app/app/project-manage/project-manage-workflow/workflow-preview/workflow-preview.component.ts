import { Component, OnInit, Input, AfterViewInit, ViewChild, ElementRef, OnChanges, SimpleChanges, Optional } from '@angular/core';
import { Workflow } from '../../workflow';
import { Subject } from 'rxjs';
import { NzModalRef } from 'ng-zorro-antd';

@Component({
  selector: 'app-workflow-preview',
  templateUrl: './workflow-preview.component.html',
  styleUrls: ['./workflow-preview.component.css']
})
export class WorkflowPreviewComponent implements OnInit, AfterViewInit, OnChanges {
  @Input()
  w: Workflow;
  view = [innerWidth / 2, 400];
  update$: Subject<boolean> = new Subject();
  links = [];
  nodes = [];
  @ViewChild('graph', { static: true })
  graph: ElementRef;
  constructor(@Optional() private modal: NzModalRef) { }

  ngAfterViewInit() {
    this.view = [this.graph.nativeElement.offsetWidth, innerHeight / 3];
    if (this.modal != null)
      this.modal.afterOpen.subscribe(e => {
        this.view = [this.graph.nativeElement.offsetWidth, innerHeight / 3];
      });
  }

  ngOnInit() {
    this.showGraph();
  }

  ngOnChanges(changes: SimpleChanges) {
    this.showGraph();
  }

  showGraph() {
    this.links = [];
    this.nodes = [];
    this.w.workflowSteps.forEach(step => {
      let color = '#545454';
      let textColor = '#ffffff';
      switch (step.issueStatus.issueLifeCycle) {
        case 'TODO':
          color = '#EBECF0';
          textColor = '#43536D';
          break;
        case 'INPROGRESS':
          color = '#DFECFE';
          textColor = '#0F4AA4';
          break;
        case 'DONE':
          color = '#E4FCEF';
          textColor = '#0A6645';
          break;
        case 'HIGHLIGHT':
          color = '#ffe8d3cc';
          textColor = '#EE7203';
          break;
        case 'ALERT':
          color = '#e6253c33';
          textColor = '#E6253C';
          break;
        case 'REVIEW':
          color = '#EAE7FE';
          textColor = '#403692';
          break;
      }
      this.nodes.push({
        id: 'node' + step.id,
        label: step.issueStatus.name,
        color: color,
        textColor: textColor
      });
      if (step.id == this.w.defaultStep.id) {
        this.nodes.push({
          id: 'node-start',
          label: ' ',
          color: '#969696',
          textColor: '#fff',
        });
        this.links.push({
          id: 'step_start',
          source: 'node-start',
          target: 'node' + step.id,
          label: ' ',
        });
      }
    });
    this.w.workflowTransitions.forEach(step => {
      if (step.fromAll) {
        this.nodes.push({
          id: 'node-all-' + step.id,
          label: 'ALL',
          color: '#000',
          textColor: '#fff',
        });
        this.links.push({
          id: 'step' + step.id,
          source: 'node-all-' + step.id,
          target: 'node' + step.toStep.id,
          label: step.name
        });
      } else {
        this.links.push({
          id: 'step' + step.id,
          source: 'node' + step.fromStep.id,
          target: 'node' + step.toStep.id,
          label: step.name
        });
      }
    });
    this.update$.next(true);
  }

  updateGraph() {
    this.showGraph();
  }

}
