package com.ariseontech.joindesk.project.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.project.domain.Group;
import com.ariseontech.joindesk.project.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = "application/json", consumes = "application/json",value = SystemInfo.apiPrefix)
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthService authService;

    @GetMapping("project/{project_key}/group")
    public String getGroups(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base,-user_detail", groupService.getAllGroupsForProject(projectKey));
    }

    @GetMapping("project/{project_key}/group/{group_id}")
    public String getGroup(@PathVariable("project_key") String projectKey, @PathVariable("group_id") Long groupId) {
        return HelperUtil.squiggly("base,-user_detail", groupService.getGroupForProject(projectKey, groupId));
    }

    @PostMapping(value = "project/{project_key}/group/create")
    public String createGroup(@PathVariable("project_key") String projectKey, @RequestBody Group group) {
        return HelperUtil.squiggly("base,-user_detail", groupService.createGroup(projectKey, group));
    }

    @PutMapping(value = "project/{project_key}/group/{group_id}/updategroup")
    public String updateGroup(@PathVariable("project_key") String projectKey, @PathVariable("group_id") Long groupId, @RequestBody Group group) {
        return HelperUtil.squiggly("base,lead[base,user_detail],group_detail", groupService.updateGroupDetails(projectKey, groupId, group));
    }

    @PutMapping(value = "project/{project_key}/group/{group_id}/update")
    public String updateGroup(@PathVariable("project_key") String projectKey, @PathVariable("group_id") Long groupId, @PathVariable("field") String field, @PathVariable("value") String value) {
        return HelperUtil.squiggly("base,-user_detail", groupService.updateProject(projectKey, groupId, field, value));
    }

    @RequestMapping(method = RequestMethod.POST, value = "project/{project_key}/group/{group_id}/remove")
    public void removeGroup(@PathVariable("project_key") String projectKey, @PathVariable("group_id") Long groupId, @RequestBody Group group) {
        groupService.removeGroup(projectKey, groupId, group);
    }

    @GetMapping("project/manage/authoritycodes")
    public String getAuthorityCodes() {
        if (!authService.hasAuthority(AuthorityCode.PROJECT_MANAGE)) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return HelperUtil.squiggly("-user_details", groupService.getAuthorityCodes());
    }

    @GetMapping("project/manage/members")
    public String getMembers() {
        if (!authService.hasAuthority(AuthorityCode.PROJECT_MANAGE)) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return HelperUtil.squiggly("-user_details", authService.getMembers());
    }
}
