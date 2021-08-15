import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UsermediaComponent } from './app/myaccount/usermedia/usermedia.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
//import { ChartsModule } from 'ng2-charts';
import { HttpClientModule } from '@angular/common/http';
import { QRCodeModule } from 'angularx-qrcode';
import { OverlayModule } from '@angular/cdk/overlay';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { WorkflowPreviewComponent } from './app/project-manage/project-manage-workflow/workflow-preview/workflow-preview.component';
import { ProjectManageComponent } from './app/project-manage/project-manage.component';
import { TimeAgoPipe } from 'time-ago-pipe';
import { SafeHtmlPipe, TaskFilterPipe } from './app/issue/issue-view/issue-view.component';
import { BoardFilterPipe } from './app/dashboard/board/board-view/board-view.component';
import { ContentViewDirective } from './general/content-view.directive';
import { IconComponent } from './general/icon/icon.component';
import { JDUserFilterPipe } from './general/userFilter.pipe';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzPageHeaderModule } from 'ng-zorro-antd/page-header';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzNotificationModule } from 'ng-zorro-antd/notification';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzMessageModule } from 'ng-zorro-antd/message';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzAffixModule } from 'ng-zorro-antd/affix';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzStepsModule } from 'ng-zorro-antd/steps';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzMentionModule } from 'ng-zorro-antd/mention';

@NgModule({
  declarations: [
    UsermediaComponent,
    IconComponent,
    WorkflowPreviewComponent,
    ProjectManageComponent,
    TimeAgoPipe,
    SafeHtmlPipe,
    JDUserFilterPipe,
    BoardFilterPipe,
    TaskFilterPipe,
    ContentViewDirective
  ],
  exports: [
    NzButtonModule,
    NzIconModule,
    NzDropDownModule,
    NzCardModule,
    NzModalModule,
    NzAffixModule,
    NzAvatarModule,
    NzTableModule,
    NzLayoutModule,
    NzInputModule,
    NzRadioModule,
    NzStatisticModule,
    NzNotificationModule,
    NzMessageModule,
    NzToolTipModule,
    NzTagModule,
    NzStepsModule,
    NzMentionModule,
    NzSkeletonModule,
    NzTabsModule,
    NzDividerModule,
    NzDescriptionsModule,
    NzGridModule,
    NzFormModule,
    NzEmptyModule,
    NzPageHeaderModule,
    NzCheckboxModule,
    NzDatePickerModule,
    NzSwitchModule,
    NzSelectModule,
    NzSpinModule,
    UsermediaComponent,
    IconComponent,
    WorkflowPreviewComponent,
    ProjectManageComponent,
    HttpClientModule,
    QRCodeModule,
    OverlayModule,
    MatSelectModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    NgxGraphModule,
    DragDropModule,
    TimeAgoPipe,
    SafeHtmlPipe,
    JDUserFilterPipe,
    BoardFilterPipe,
    TaskFilterPipe,
    ContentViewDirective
  ],
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzIconModule,
    NzDropDownModule,
    NzCardModule,
    NzModalModule,
    NzAffixModule,
    NzAvatarModule,
    NzTableModule,
    NzLayoutModule,
    NzDividerModule,
    NzInputModule,
    NzRadioModule,
    NzStatisticModule,
    NzNotificationModule,
    NzMessageModule,
    NzToolTipModule,
    NzTagModule,
    NzStepsModule,
    NzMentionModule,
    NzSkeletonModule,
    NzTabsModule,
    NzDescriptionsModule,
    NzGridModule,
    NzFormModule,
    NzEmptyModule,
    NzPageHeaderModule,
    NzCheckboxModule,
    NzDatePickerModule,
    NzSwitchModule,
    NzSelectModule,
    NzSpinModule,
    ReactiveFormsModule,
    RouterModule,
    HttpClientModule,
    QRCodeModule,
    OverlayModule,
    MatDialogModule,
    MatSelectModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    NgxGraphModule,
    DragDropModule,
  ]
})
export class SharedModule { }
