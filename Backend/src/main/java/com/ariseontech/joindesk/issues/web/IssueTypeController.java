package com.ariseontech.joindesk.issues.web;


import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.domain.IssueType;
import com.ariseontech.joindesk.issues.service.IssueTypeService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = "application/json", consumes = "application/json", value = SystemInfo.apiPrefix + "/manage/project/{project_key}/issue_type")
public class IssueTypeController {

    @Autowired
    private IssueTypeService issueTypeService;

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public String getAllIssueTypes(@PathVariable("project_key") String projectKey, @RequestParam(value = "include_inactive", defaultValue = "true") boolean includeInactive) {
        return HelperUtil.squiggly("base,issue_type_detail", issueTypeService.getAll(projectKey, includeInactive));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/statuses")
    public String getAllStatuses() {
        return HelperUtil.squiggly("base,workflow_detail", issueTypeService.getAllStatus());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{issueTypeID}/")
    public String getAllIssueTypes(@PathVariable("project_key") String projectKey, @PathVariable("issueTypeID") Long issueTypeID) {
        return HelperUtil.squiggly("base,issue_type_detail", issueTypeService.findOne(issueTypeID).get());
    }

    @RequestMapping(method = RequestMethod.POST, value = "create")
    public String createIssueType(@PathVariable("project_key") String projectKey, @RequestBody IssueType issueType) {
        return HelperUtil.squiggly("base,issue_type_detail", issueTypeService.create(projectKey, issueType));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/change/{id}/workflow/{workflow_id}")
    public String changeWorkflow(@PathVariable("id") Long id, @PathVariable("workflow_id") Long workflow_id) {
        return HelperUtil.squiggly("base,issue_type_detail", issueTypeService.changeWorkflow(id, workflow_id));
    }

    //TODO Post for change workflow

    @RequestMapping(method = RequestMethod.PUT, value = "/update/{id}")
    public String update(@PathVariable("id") Long issueTypeID, @RequestBody IssueType it) {
        return HelperUtil.squiggly("base,issue_type_detail", issueTypeService.update(issueTypeID, it));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "remove")
    public String removeIssueType(@RequestParam("id") Long id) {
        issueTypeService.deleteIssueType(id);
        return new JSONObject().put("success", true).toString();
    }

}
