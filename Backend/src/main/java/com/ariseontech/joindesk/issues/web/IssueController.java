package com.ariseontech.joindesk.issues.web;


import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.issues.service.RelationshipService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(produces = "application/json", value = SystemInfo.apiPrefix + "/issue/")
public class IssueController {

    @Autowired
    private IssueService issueService;
    @Autowired
    private RelationshipService relationshipService;

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public String getBaseFilter(@RequestParam(name = "p", required = false) String projectKey, @RequestParam(name = "f", required = false) Long filterId) {
        return HelperUtil.squiggly("base,possible*,*[base]", issueService.getBaseFilter(projectKey, filterId));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/filters")
    public String getFilters(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base,possible*,*[base]", issueService.getFilters(projectKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/filters/open")
    public String getopenFilters(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base,possible*,*[base]", issueService.getOpenFilters(projectKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/find")
    public String findAllIssues(@RequestBody IssueFilterDTO filter) {
        return HelperUtil.squiggly("base", issueService.findAll(filter));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/searchl")
    public String searchIssuesLucene(@PathVariable("project_key") String projectKey, @RequestBody IssueSearchDTO issueSearchDTO) {
        return HelperUtil.squiggly("base,issues[audit_details]", issueService.searchIssuesLucene(projectKey, issueSearchDTO));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/searchDue")
    public String searchDueIssues(@PathVariable("project_key") String projectKey, @RequestParam(value = "from") String from
            , @RequestParam(value = "to") String to) {
        return HelperUtil.squiggly("base", issueService.searchDueIssues(projectKey, from, to));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/searchBetween")
    public String searchIssuesBetween(@PathVariable("project_key") String projectKey, @RequestParam(value = "from") String from
            , @RequestParam(value = "to") String to) {
        return HelperUtil.squiggly("base", issueService.searchIssuesBetween(projectKey, from, to));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/filter/save")
    public String saveFilter(@PathVariable("project_key") String projectKey, @RequestBody IssueFilter filter) {
        return HelperUtil.squiggly("base,issues[audit_details]", issueService.saveFilter(projectKey, filter));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/search")
    public String searchIssues(@PathVariable("project_key") String projectKey, @RequestParam(value = "q") String q) {
        return HelperUtil.squiggly("base", issueService.searchIssues(projectKey, q));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/assignedToMe")
    public String assignedToMe() {
        return HelperUtil.squiggly("base", issueService.assignedToMe());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/due")
    public String due(@RequestParam(name = "days", required = false) Integer days) {
        return HelperUtil.squiggly("base", issueService.due(days));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}")
    public String getIssue(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        Issue issue = issueService.get(projectKey, issueKey);
        issueService.registerIssueView(issue);
        return HelperUtil.squiggly("base,issue_detail,audit_details,project[base,project_detail,audit_details]", issue);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/attachments")
    public String getIssueAttachments(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.getAttachments(projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/workflow")
    public String getIssueWorkflow(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail,-audit_details,workflow_detail", issueService.getIssueWorkflow(projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/history")
    public String getIssueHistory(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.getHistory(projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/attachment/{attachment_name}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getAttachment(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @PathVariable("attachment_name") Long attachmentName) throws IOException {
        return issueService.getAttachment(projectKey, issueKey, attachmentName);
    }

    @RequestMapping(value = "/{project_key}/{issue_key}/attachment/preview/{attachment_name}/", method = RequestMethod.GET)
    public ResponseEntity<byte[]> previewAttachment(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @PathVariable("attachment_name") String attachmentName) {
        return issueService.getAttachmentPreview(projectKey, issueKey, attachmentName);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_id}/add")
    public String addIssue(@RequestBody Issue issue, @PathVariable("project_id") Long projectID) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.create(issue, projectID));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_key}/{issue_key}/update", consumes = MediaType.ALL_VALUE)
    public String updateIssue(@RequestBody Issue issue, @PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.update(issue, issueKey, projectKey));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/quick_update")
    public String quickUpdateIssue(@RequestBody QuickUpdate data) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.quickUpdate(data));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_key}/{issue_key}/updateCustomField")
    public String updateIssueCustomField(@RequestBody IssueCustomField customField, @PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail", issueService.updateCustomField(customField, issueKey, projectKey));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_key}/{issue_key}/delete")
    public void deleteIssue(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        issueService.delete(issueKey, projectKey);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_key}/{issue_key}/change_type")
    public void changeIssueType(@RequestBody IssueType issueType, @PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        issueService.changeIssueType(issueType, issueKey, projectKey);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/attach")
    public String attach(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestParam("file") MultipartFile file) throws IOException {
        return HelperUtil.squiggly("base", issueService.saveAttachment(file, projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/detach")
    public void detach(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Attachment attachment) {
        issueService.deleteAttachment(attachment, projectKey, issueKey);
    }

    /* Label */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/labels")
    public String getLabels(@PathVariable("project_key") String projectKey, @RequestParam("q") String q) {
        return HelperUtil.squiggly("base", issueService.getLabels(projectKey, q));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/addLabel")
    public String addLabel(@PathVariable("project_key") String projectKey, @RequestParam("q") String q) {
        return HelperUtil.squiggly("base", issueService.addLabels(projectKey, q));
    }

    /* Comments */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/comments")
    public String getIssueComments(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.getComments(projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/comment")
    public String comment(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Comment comment) {
        return HelperUtil.squiggly("base", issueService.saveComment(comment, projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/comment/delete")
    public String commentDelete(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Comment comment) {
        issueService.deleteComment(comment, projectKey, issueKey);
        return new JSONObject().toString();
    }

    /* Task */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/task")
    public String getIssueTask(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", issueService.getTasks(projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/task")
    public String saveTask(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Task task) {
        return HelperUtil.squiggly("base", issueService.saveTask(task, projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_key}/{issue_key}/task/complete")
    public String completeTask(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Task task) {
        return HelperUtil.squiggly("base", issueService.completeTask(task, projectKey, issueKey));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_key}/{issue_key}/task/reorder")
    public void reorderTask(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody List<Task> tasks) {
        issueService.reorderTask(tasks, projectKey, issueKey);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/task/delete")
    public String taskDelete(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Task task) {
        issueService.deleteTask(task, projectKey, issueKey);
        return new JSONObject().toString();
    }

    /* Relationships */
    @RequestMapping(method = RequestMethod.GET, value = "/relationship_types")
    public String getRelationships() {
        return HelperUtil.squiggly("base", relationshipService.getAll());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/relationships")
    public String getIssueRelationships(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base", relationshipService.getByIssue(issueService.get(projectKey, issueKey)));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/o_relationships")
    public String getIssueOtherRelationships(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base", relationshipService.getOthersByIssue(issueService.get(projectKey, issueKey)));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/mentions")
    public String getIssueMentions(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey) {
        return HelperUtil.squiggly("base", relationshipService.getMentionsByIssue(issueService.get(projectKey, issueKey)));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/relationships/add")
    public String addIssueRelationship(@RequestBody IssueRelationship issueRelationship) {
        return HelperUtil.squiggly("base", relationshipService.saveIssueRelationship(issueRelationship));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/relationships/remove")
    public void removeIssueRelationship(@RequestBody IssueRelationship issueRelationship) {
        relationshipService.removeIssueRelationship(issueRelationship);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/o_relationships/add")
    public String addIssueWebRelationship(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody IssueOtherRelationship r) {
        return HelperUtil.squiggly("base", relationshipService.saveIssueOtherRelationship(projectKey, issueKey, r));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/o_relationships/remove")
    public void removeIssueWebRelationship(@RequestBody IssueOtherRelationship r) {
        relationshipService.removeIssueOtherRelationship(r);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_key}/{issue_key}/transition/{transition_id}")
    public String updateIssueTransition(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey,
                                        @PathVariable("transition_id") Long transitionID, @RequestBody String payload) {
        return HelperUtil.squiggly("base,issue_detail,audit_details,project[base,project_detail,audit_details]",
                issueService.transition(issueKey, projectKey, transitionID, payload));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/{issue_key}/lane_transition/{lane_id}")
    public void updateIssueTransitionFromLane(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey,
                                              @PathVariable("lane_id") Long laneId) {
        issueService.laneTransition(issueKey, projectKey, laneId);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "remove/{id}")
    public String removeIssue(@PathVariable("id") Long issueID) {
        issueService.delete(issueID);
        return new JSONObject().put("success", true).toString();
    }

    @RequestMapping(method = RequestMethod.POST, value = "{project_key}/export")
    public byte[] exportIssues(@PathVariable("project_key") String projectKey, @RequestParam("type") String type,
                               @RequestBody IssueSearchDTO filter, HttpServletResponse response, HttpServletRequest request) throws Exception {
        return issueService.export(projectKey, filter, request, response, type);
    }

    /* Watchers */
    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/watcher/add")
    public String addWatcher(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Login watcher) {
        return HelperUtil.squiggly("base,issue_detail", issueService.addWatcherAndGet(issueService.get(projectKey, issueKey), watcher));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/{issue_key}/watcher/remove")
    public String removeWatcher(@PathVariable("project_key") String projectKey, @PathVariable("issue_key") Long issueKey, @RequestBody Login watcher) {
        return HelperUtil.squiggly("base,issue_detail", issueService.removeWatcherAndGet(issueService.get(projectKey, issueKey), watcher));
    }
}