### Workflow view

![](https://i.postimg.cc/Pq1wntF3/image.png)

### Workflow edit
![](https://i.postimg.cc/fLmnvPy0/image.png)

#### Add Step
Select existing status or new status to specific new status name and category

Step category provides different background colors to status to visualize stage of issue
**Category**
* TODO - Grey
* INPROGRESS - Blue
* DONE - Green
* REVIEW - Purple
* HIGHLIGHT - Orange
* ALERT - Red

#### Add Transaction
Create transaction to define flow from one status to another


#### Transition Pre-Condition
Click on transition name to specify condition on when a transition can be preformed.
The selected pre-conditions should be satisfied to make a transition.

* Current user => If current user is someone from list
* Is in Group => If current user belongs to group
* Has Permission => If current user has specific permission
* Field Required => If select field is filled
* Checklist completed => If all checklist items are completed

#### Transition Post-Condition
Click on transition name to specify post condition which will run after a transaction is processed.

* Assign to user => reassign issue based on given logic
* Update field => update issue field based on given logic