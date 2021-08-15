package com.ariseontech.joindesk.project.web;

import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.scheduler.service.JDSchedulerService;
import com.ariseontech.joindesk.slack.SlackService;
import org.json.JSONObject;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.time.ZoneId;

@RestController
@RequestMapping(SystemInfo.apiPrefix + "/app/config")
public class AppConfigController {

    @Autowired
    private AuthService authService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private SlackService slackService;
    @Autowired
    private JDSchedulerService jdSchedulerService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getAppConfiguration() {
        if (!authService.isSuperAdmin())
            throw new JDException("Invalid Access", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        JSONObject c = new JSONObject();
        c.put("start", configurationService.getString(ConfigurationService.JDCONFIG.APP_BUSINESS_START_TIME));
        c.put("end", configurationService.getString(ConfigurationService.JDCONFIG.APP_BUSINESS_END_TIME));
        c.put("timezone", configurationService.getString(ConfigurationService.JDCONFIG.APP_TIMEZONE));
        c.put("baseUrl", configurationService.getString(ConfigurationService.JDCONFIG.APP_BASE_URL));
        c.put("slackEnabled", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED));
        String token = configurationService.getString(ConfigurationService.JDCONFIG.APP_SLACK_ACCESS_TOKEN);
        c.put("slackConnected", null != token && !token.isEmpty());
        c.put("slackClientID", slackService.getClientID());
        c.put("selfRegistrationEmailEnabled", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_EMAIL_ENABLED));
        c.put("selfRegistrationSlackEnabled", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_SLACK_ENABLED));
        c.put("selfRegistrationDomains", configurationService.getString(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_ALLOWED_DOMAINS));
        c.put("selfRegistrationRequireEmailReview", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_EMAIL));
        c.put("selfRegistrationRequireSlackReview", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_SLACK));
        c.put("timezones", ZoneId.getAvailableZoneIds());
        return c.toString();
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public void saveAppConfiguration(@RequestBody String confString) throws ParseException, SchedulerException {
        if (!authService.isSuperAdmin())
            throw new JDException("Authorization Failure", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        JSONObject conf = new JSONObject(confString);
        configurationService.save(ConfigurationService.JDCONFIG.APP_BUSINESS_START_TIME, conf.getString("start"));
        configurationService.save(ConfigurationService.JDCONFIG.APP_BUSINESS_END_TIME, conf.getString("end"));
        configurationService.save(ConfigurationService.JDCONFIG.APP_TIMEZONE, conf.getString("timezone"));
        configurationService.save(ConfigurationService.JDCONFIG.APP_BASE_URL, conf.getString("baseUrl"));
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED, conf.getBoolean("slackEnabled"));
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_EMAIL_ENABLED, conf.getBoolean("selfRegistrationEmailEnabled"));
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_SLACK_ENABLED, conf.getBoolean("selfRegistrationSlackEnabled"));
        configurationService.save(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_ALLOWED_DOMAINS, conf.getString("selfRegistrationDomains"));
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_EMAIL, conf.getBoolean("selfRegistrationRequireEmailReview"));
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_SLACK, conf.getBoolean("selfRegistrationRequireSlackReview"));
        jdSchedulerService.scheduleDueDateJob();
    }

}
