package com.ariseontech.joindesk.auth.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.domain.PreferredAuthTypes;
import com.ariseontech.joindesk.auth.domain.Token;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.auth.util.View;
import com.ariseontech.joindesk.event.service.EmailService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.repo.ReportDTO;
import com.ariseontech.joindesk.issues.service.WorkLogService;
import com.fasterxml.jackson.annotation.JsonView;
import com.slack.api.methods.SlackApiException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/my")
public class MyAccountController {

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentLogin currentLogin;

    @Autowired
    private WorkLogService workLogService;

    @Autowired
    private EmailService emailService;

    @JsonView(View.Details.class)
    @RequestMapping(method = RequestMethod.GET, value = "")
    public Login get() {
        return userService.get(currentLogin.getUser().getId());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/update")
    @JsonView(View.Details.class)
    public Login update(@RequestBody Login login) {
        if (!login.equals(currentLogin.getUser())) {
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        return userService.save(login.getId(), login.getFullName(), login.getEmail(), currentLogin.getUser().isSuperAdmin(), login.isEmailNotification(), login.isSlackNotification());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/removePic")
    public void removePic() {
        userService.removePic(currentLogin.getUser().getId());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/attach")
    @JsonView(View.Details.class)
    public Login attach(@RequestParam("file") MultipartFile file) throws IOException {
        return userService.savePic(file, currentLogin.getUser().getId());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/change_password")
    public String changePwd(@RequestBody String d) {
        JSONObject data = new JSONObject(d);
        return userService.changePassword(data.getString("old_password"), data.getString("password"), data.getString("confirm_password"));
    }

    @RequestMapping(method = RequestMethod.GET, value = "active")
    public Set<Token> getSessions() {
        return userService.getActiveSessions(currentLogin.getUser().getId());
    }

    @RequestMapping(method = RequestMethod.POST, value = "sign_out_session")
    public void removeSession(@RequestBody Token t) {
        userService.removeActiveSession(t);
    }

    @RequestMapping(method = RequestMethod.POST, value = "sign_out_sessions")
    public void removeOtherSessions() {
        userService.removeAllActiveSessions();
    }

    @RequestMapping(method = RequestMethod.GET, value = "slackConnectInit")
    public void slackConnectInit(@RequestParam(value = "username") String username) throws IOException, SlackApiException {
        userService.requestSlackConnect(username);
    }

    @RequestMapping(method = RequestMethod.GET, value = "slackConnect")
    public void slackConnect(@RequestParam(value = "username") String username, @RequestParam(value = "token") String token) throws IOException, SlackApiException {
        userService.slackConnect(username, token);
    }

    @RequestMapping(method = RequestMethod.GET, value = "slackDisconnect")
    public void slackDisconnect() {
        userService.slackDisconnect();
    }

    @RequestMapping(method = RequestMethod.GET, value = "activateMFA")
    public void activateMfa(@RequestParam(value = "code") int token) {
        userService.activateMfa(token);
    }

    @RequestMapping(method = RequestMethod.GET, value = "deactivateMFA")
    public void deactivateMFA() {
        userService.deactivateMFA();
    }

    @RequestMapping(method = RequestMethod.GET, value = "setPreferredAuth")
    public void setPreferredAuth(@RequestParam(value = "type") PreferredAuthTypes type) {
        userService.setPreferredAuth(type);
    }

    /* Work Log */
    @RequestMapping(method = RequestMethod.GET, value = "workLog")
    public ReportDTO getWeekLogged() {
        return workLogService.weekOverview();
    }

    @RequestMapping(method = RequestMethod.GET, value = "workLogReport")
    public String workLogReport(@RequestParam("start") String start, @RequestParam("end") String end) {
        return HelperUtil.squiggly("base", workLogService.report(start, end));
    }

}