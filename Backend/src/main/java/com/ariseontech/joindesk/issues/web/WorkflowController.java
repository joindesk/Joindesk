package com.ariseontech.joindesk.issues.web;


import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.service.WorkflowService;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = "application/json", consumes = "application/json", value = SystemInfo.apiPrefix + "/manage/project/{project_key}/workflow/")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ProjectService projectService;

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public String getAllWorkflows(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.getAll(projectKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/except/{workflow_id}")
    public String getAllWorkflowsExcept(@PathVariable("project_key") String projectKey, @PathVariable("workflow_id") Long workflowID) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.getAll(projectKey).stream().filter(w -> !w.getId().equals(workflowID)).collect(Collectors.toList()));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public String getWorkflow(@PathVariable("id") Long workflowID) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.findWorkflow(workflowID).get());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public String create(@RequestBody Workflow workflow, @PathVariable("project_key") String projectKey) {
        workflow.setProject(projectService.findByKey(projectKey));
        return HelperUtil.squiggly("base,workflow_detail", workflowService.create(workflow));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/update/{id}")
    public String update(@PathVariable("id") Long workflowID, @RequestBody Workflow workflow) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.update(workflowID, workflow.getName(), workflow.getDescription(), workflow));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{id}/step/add")
    public String addStep(@PathVariable("id") Long workflowID, @RequestBody WorkflowStep step) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.addStep(workflowID, step));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{id}/step/update")
    public String updateStep(@PathVariable("id") Long workflowID, @RequestBody WorkflowStep step) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.updateStep(workflowID, step));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/step/remove/{step_id}")
    public String removeStep(@PathVariable("id") Long workflowID, @PathVariable("step_id") Long stepID) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.removeStep(workflowID, stepID));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{id}/transition/add")
    public String addTransition(@PathVariable("id") Long workflowID, @RequestBody WorkflowTransition transition) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.addTransition(workflowID, transition.getName(), transition.isFromAll() ? 0 : transition.getFromStep().getId(), transition.getToStep().getId()));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{id}/transition/update")
    public String updateTransition(@PathVariable("id") Long workflowID, @RequestBody WorkflowTransition transition) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.updateTransition(workflowID, transition.getId(), transition.getName()));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/transition/remove/{transition_id}")
    public String removeTransition(@PathVariable("id") Long workflowID, @PathVariable("transition_id") Long transitionID) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.removeTransition(workflowID, transitionID));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "remove")
    public String removeWorkflow(@RequestParam("id") Long id) {
        workflowService.deleteWorkflow(id);
        return new JSONObject().put("success", true).toString();
    }

    /* Transition Properties */
    @RequestMapping(method = RequestMethod.GET, value = "transition/properties")
    public String getTransitionProperties() {
        return workflowService.getTransitionProperties();
    }

    @RequestMapping(method = RequestMethod.GET, value = "{workflow_id}/transition/{transition_id}/properties/{sub_type}")
    public String getTransitionPropertiesFields(@PathVariable("workflow_id") Long workflowID, @PathVariable("transition_id") Long transitionID, @PathVariable("sub_type") WorkflowTransitionPropertySubTypes subType) {
        return HelperUtil.squiggly("base", workflowService.getWorkflowTransitionPropertiesFields(workflowID, transitionID, subType));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{workflow_id}/transition/{transition_id}/properties")
    public String getAllTransitionProperties(@PathVariable("workflow_id") Long workflowID, @PathVariable("transition_id") Long transitionID) {
        return workflowService.getWorkflowTransitionProperties(workflowID, transitionID);
    }

    @RequestMapping(method = RequestMethod.POST, value = "{workflow_id}/transition/{transition_id}/properties/save")
    public String saveTransitionProperty(@PathVariable("workflow_id") Long workflowID, @RequestBody WorkflowTransitionProperties workflowTransitionProperties, @PathVariable("transition_id") Long transitionID) {
        return HelperUtil.squiggly("base,workflow_detail", workflowService.updateWorkflowTransitionProperties(workflowID, transitionID, workflowTransitionProperties));
    }

    @RequestMapping(method = RequestMethod.POST, value = "{workflow_id}/transition/{transition_id}/properties/remove")
    public String removeTransitionProperty(@PathVariable("workflow_id") Long workflowID, @RequestBody WorkflowTransitionProperties workflowTransitionProperties, @PathVariable("transition_id") Long transitionID) {
        workflowService.removeWorkflowTransitionProperties(workflowID, transitionID, workflowTransitionProperties);
        return new JSONObject().put("success", true).toString();
    }
}
