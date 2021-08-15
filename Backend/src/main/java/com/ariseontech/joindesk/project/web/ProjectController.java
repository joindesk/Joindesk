package com.ariseontech.joindesk.project.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.domain.Version;
import com.ariseontech.joindesk.issues.service.VersionService;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.domain.TimeTracking;
import com.ariseontech.joindesk.project.service.ProjectService;
import com.slack.api.methods.SlackApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/project", produces = "application/json", consumes = "application/json")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private VersionService versionService;

    @GetMapping("/")
    public String projects() {
        return HelperUtil.squiggly("-user_detail,project_detail", projectService.getAllProjects());
    }

    @GetMapping("/{project_id}/")
    public String getProject(@PathVariable("project_id") Long projectId) {
        return HelperUtil.squiggly("-user_details,project_detail,audit_details", projectService.getProject(projectId));
    }

    @GetMapping("/key/{project_key}/")
    public String getProjectByKey(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("-user_details,project_detail,audit_details", projectService.getProjectByKey(projectKey));
    }

    @GetMapping("/overview")
    public String getProjectOverview() {
        return HelperUtil.squiggly("-user_details,project_overview", projectService.getProjectsOverview());
    }

    @PostMapping("/create/")
    public String createProject(@Valid @RequestBody Project project) {
        return HelperUtil.squiggly("base,lead[base,user_detail],project_detail,audit_details", projectService.createProject(project));
    }

    @PostMapping("/update/{project_id}")
    public String updateProject(@PathVariable("project_id") Long projectId, @RequestBody Project project) {
        return HelperUtil.squiggly("base,lead[base,user_detail],project_detail,audit_details", projectService.updateProject(projectId, project));
    }

    @GetMapping("/{project_key}/members")
    public String getMembersByKey(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("-user_details", projectService.getMembersByProjectKey(projectKey));
    }

    @GetMapping("/{project_key}/timetracking")
    public String getTimeTrackingSettings(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base", projectService.getTimeTrackingSettings(projectService.getProjectByKey(projectKey)));
    }

    @PostMapping("/{project_key}/timetracking/update")
    public String updateTimeTrackingSettings(@PathVariable("project_key") String projectKey, @RequestBody TimeTracking timeTracking) {
        return HelperUtil.squiggly("base", projectService.updateTimeTrackingSettings(timeTracking));
    }

    //Version management
    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/version")
    public String getVersions(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base", versionService.getAllVersionForProject(projectKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{project_key}/version/{version_id}")
    public String getVersions(@PathVariable("project_key") String projectKey, @PathVariable("version_id") Long versionID) {
        return HelperUtil.squiggly("base", versionService.getVersionForProject(projectKey, versionID));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/version/save")
    public String saveVersion(@RequestBody Version v, @PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base,-user_detail", versionService.save(projectKey, v));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/version/remove")
    public void removeVersion(@RequestBody Version v, @PathVariable("project_key") String projectKey) {
        versionService.remove(projectKey, v);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{project_key}/version/release")
    public void releaseVersion(@RequestBody Version v, @PathVariable("project_key") String projectKey) {
        versionService.release(projectKey, v);
    }

    @GetMapping("/channels")
    public String getSlackChannels() throws IOException, SlackApiException {
        return HelperUtil.squiggly("base", projectService.getChannels());
    }
}
