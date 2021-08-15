import { Injectable } from "@angular/core";
import { NzNotificationService, NzModalService, NzMessageService } from "ng-zorro-antd";
import { Observable } from "rxjs";

@Injectable()
export class JDToastrService {
  constructor(private notification: NzNotificationService, private modalService: NzModalService,
    private message: NzMessageService) { }

  success(subject: string, message: string) {
    this.notification.success(subject, message);
  }
  info(subject: string, message: string) {
    this.notification.info(subject, message);
  }
  warn(subject: string, message: string) {
    this.notification.warning(subject, message);
  }
  error(subject: string, message: string) {
    this.notification.error(subject, message);
  }

  successMessage(msg: string) {
    this.message.success(msg);
  }
  infoMessage(msg: string) {
    this.message.info(msg);
  }
  warnMessage(msg: string) {
    this.message.warning(msg);
  }
  errorMessage(msg: string) {
    this.message.error(msg);
  }

  confirm(text: string) {
    let result = {
      value: false
    }
    let data = new Observable<any>(observer => {
      this.modalService.confirm({
        nzTitle: 'Are you sure ?',
        nzContent: text,
        nzOnOk: () => {
          result.value = true;
          observer.next(result);
          observer.complete();
        },
        nzOnCancel: () => {
          result.value = false;
          observer.next(result);
          observer.complete();
        }
      });
    });
    return data.toPromise();
  }
}
