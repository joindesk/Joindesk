package com.ariseontech.joindesk.event.web;

import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.slack.SlackService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/slack/event", consumes = "application/json", produces = "application/json")
public class SlackEventController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SlackService slackService;

    @PostMapping
    public String receive(@RequestBody String rawPayload) {
        if (!configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED)) {
            return "";
        }
        JSONObject payload = new JSONObject(rawPayload);
        JSONObject result = new JSONObject();
        System.out.println(payload);
        if (payload.getString("type").equalsIgnoreCase("url_verification")) {
            result.put("challenge", payload.getString("challenge"));
        } else if (payload.has("event")) {
            switch (payload.getJSONObject("event").getString("type")) {
                case "tokens_revoked":
                case "app_uninstalled":
                    slackService.disconnect();
                    break;
                default:
                    slackService.handleEvent(payload.getJSONObject("event").getString("type"), payload);
                    break;
            }
        }
        return result.toString();
    }
}
