import { Directive, AfterViewInit, Renderer2, ElementRef, Input } from '@angular/core';

@Directive({
  selector: '[appContentView]'
})
export class ContentViewDirective implements AfterViewInit {

  @Input() set appContentView(content: string) {
    this._element.innerHTML = content || '';
  }

  private _element: any;

  constructor(
    private renderer2: Renderer2,
    element: ElementRef
  ) {
    this._element = element.nativeElement;
  }

  ngAfterViewInit() {
    this.renderer2.addClass(this._element, 'contentview');
  }

}
