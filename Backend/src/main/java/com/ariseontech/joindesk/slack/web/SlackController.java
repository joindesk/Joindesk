package com.ariseontech.joindesk.slack.web;

import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.slack.SlackService;
import com.slack.api.methods.SlackApiException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/slack")
public class SlackController {
    @Autowired
    private AuthService authService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private SlackService slackService;

    @RequestMapping(value = "connect", method = RequestMethod.GET)
    public void slackConnect(HttpServletResponse response, @RequestParam(value = "code") String code) throws IOException, SlackApiException {
        slackService.connect(code);
        response.sendRedirect(configurationService.getApplicationDomain() + "admin/manage/config?slack=connected");
    }

    @PostMapping(value = "interaction", consumes = "application/x-www-form-urlencoded;charset=UTF-8", produces = "application/json")
    public void interaction(HttpServletRequest request, @RequestBody MultiValueMap<String, String> payload) throws IOException, SlackApiException {
        //System.out.println(request.getHeader("X-Slack-Signature"));
        //System.out.println(request.getHeader("X-Slack-Request-Timestamp"));
        JSONObject data = new JSONObject(payload.getFirst("payload"));
        System.out.println(data.toString(2));
        slackService.handleAction(data);
    }
}
