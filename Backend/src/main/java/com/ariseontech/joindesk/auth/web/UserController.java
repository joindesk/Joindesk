package com.ariseontech.joindesk.auth.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.service.UserService;
import com.slack.api.methods.SlackApiException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping(produces = "application/json", consumes = "application/json", value = SystemInfo.apiPrefix + "/manage/user/")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public String getAllUsers() {
        return HelperUtil.squiggly("base", userService.getAll());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/pending")
    public String getAllPendingUsers() {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.getAllPending());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public String getUser(@PathVariable("id") Long userID) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.get(userID));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public String create(@RequestBody Login login) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.createUser(login.getFullName(),
                login.getEmail(), login.getPassword(), login.isSuperAdmin(), login.isActive(), false));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/save")
    public String save(@RequestBody Login login) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.saveFromAdmin(login));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/pending/approve")
    public String approvePending(@RequestBody Login login) {
        return userService.approvePending(login);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/pending/reject")
    public String rejectPending(@RequestBody Login login) {
        return userService.rejectPending(login);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/getApiToken")
    public String getApiToken(@RequestBody Login login) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.getApiToken(login));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/resetApiToken")
    public String resetApiToken(@RequestBody Login login) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.resetApiToken(login));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search")
    public String searchByProject(@RequestParam("project") String projectKey, @RequestParam("q") String q) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.searchByProject(projectKey, q));
    }

    //Slack
    @RequestMapping(method = RequestMethod.GET, value = "/slackStatus")
    public String slackStatus() {
        return new JSONObject().put("enabled", userService.slackStatus()).toString();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/slackImportCandidate")
    public String importFromSlackCandidates() throws IOException, SlackApiException {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", userService.fetchSlackUsers());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/slackImport")
    public void importFromSlack(@RequestParam(value = "id") String id) throws IOException, SlackApiException {
        userService.importSlackUser(id);
    }
}
