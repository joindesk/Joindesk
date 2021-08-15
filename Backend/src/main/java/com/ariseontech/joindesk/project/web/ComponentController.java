package com.ariseontech.joindesk.project.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.service.VersionService;
import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.service.ComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/manage/project/{project_key}/component", produces = "application/json", consumes = "application/json")
public class ComponentController {

    @Autowired
    private ComponentService componentService;

    @Autowired
    private VersionService versionService;

    @GetMapping("/")
    public String getAll(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("-user_detail,component_detail", componentService.getAllForProject(projectKey));
    }

    @GetMapping("/{component_id}")
    public String get(@PathVariable("project_key") String projectKey, @PathVariable("component_id") Long fieldId) {
        return HelperUtil.squiggly("-user_details,component_detail", componentService.get(projectKey, fieldId));
    }

    @PostMapping("/save")
    public String save(@PathVariable("project_key") String projectKey, @RequestBody Component component) {
        return HelperUtil.squiggly("base,component_detail", componentService.save(projectKey, component));
    }

    @PostMapping("/delete")
    public void delete(@PathVariable("project_key") String projectKey, @RequestBody Component component) {
        componentService.delete(projectKey, component);
    }
}
