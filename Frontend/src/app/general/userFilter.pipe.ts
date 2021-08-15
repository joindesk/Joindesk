import { Pipe, PipeTransform } from '@angular/core';
import { JDUser } from '../app/admin/user/user';

@Pipe({
    name: 'jdUserfilter',
    pure: false
})
export class JDUserFilterPipe implements PipeTransform {
    transform(items: JDUser[], filter: string): any {
        if (!items || !filter) {
            return items;
        }
        // filter items array, items which match and return true will be
        // kept, false will be filtered out
        return items.filter(item => item.fullName.toLowerCase().indexOf(filter.toLowerCase()) !== -1);
    }
}