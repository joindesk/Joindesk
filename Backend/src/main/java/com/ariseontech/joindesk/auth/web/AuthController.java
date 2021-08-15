package com.ariseontech.joindesk.auth.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.domain.LoginRequestDTO;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.repo.TokenRepo;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;
import java.time.ZoneId;
import java.util.TimeZone;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = "application/json", value = SystemInfo.apiPrefix)
public class AuthController {

    @Autowired
    private LoginRepo loginRepo;
    @Autowired
    private TokenRepo tokenRepo;
    @Autowired
    private CurrentLogin currentLogin;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;
    @Autowired
    private ConfigurationService configurationService;

    @RequestMapping(value = "/auth/login", method = RequestMethod.POST)
    public String login(HttpServletRequest request, HttpServletResponse response, @RequestBody LoginRequestDTO loginDetail) {
        JSONObject resp = new JSONObject();
        if (null == loginDetail.getUsername() || loginDetail.getUsername().isEmpty()
                || null == loginDetail.getPassword() || loginDetail.getPassword().isEmpty()) {
            return resp.put("success", false).toString();
        }
        return authService.authenticate(loginDetail.getUsername().toLowerCase(), loginDetail.getPassword(), loginDetail.isRemember(), loginDetail.getDeviceFp(), loginDetail.getDeviceInfo(), loginDetail.getMode(), request, response);
    }

    @RequestMapping(value = "/auth/forgot", method = RequestMethod.POST)
    public String forgot(HttpServletRequest request, HttpServletResponse response, @RequestBody LoginRequestDTO loginDetail) {
        JSONObject resp = new JSONObject();
        if (ObjectUtils.isEmpty(loginDetail.getUsername())) {
            return resp.put("success", false).toString();
        }
        return authService.forgot(loginDetail, request, response);
    }

    /*@RequestMapping(value = "/auth/gLogin", method = RequestMethod.POST)
    public String gLogin(HttpServletRequest request, HttpServletResponse response, @RequestBody LoginRequest loginDetail) throws FirebaseAuthException {
        JSONObject resp = new JSONObject();
        if (null == loginDetail.getUsername() || loginDetail.getUsername().isEmpty()) {
            return resp.put("success", false).toString();
        }
        return authService.googleAuthenticate(loginDetail.getUsername(), loginDetail.getDeviceFp(), loginDetail.getDeviceInfo(), request, response);
    }*/

    @RequestMapping(value = "/auth/checkUser", method = RequestMethod.POST)
    public String checkUser(@RequestBody LoginRequestDTO loginDetail) {
        JSONObject resp = new JSONObject();
        if (ObjectUtils.isEmpty(loginDetail.getUsername())) {
            return resp.put("success", false).toString();
        }
        return authService.checkUser(loginDetail.getUsername().toLowerCase(), loginDetail.getMode());
    }

    @RequestMapping(value = "/auth/register", method = RequestMethod.GET)
    public String register() {
        boolean allowEmail = configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_EMAIL_ENABLED);
        boolean allowSlack = configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_SLACK_ENABLED);
        return new JSONObject().put("allowed", allowEmail || allowSlack).put("allowEmail", allowEmail).put("allowSlack", allowSlack)
                .put("setup", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SETUP)).toString();
    }

    @RequestMapping(value = "/auth/register", method = RequestMethod.POST)
    public String registerUser(@RequestBody Login user) {
        return userService.registerUser(user.getFullName(), user.getEmail());
    }

    @RequestMapping(value = "/auth/verify", method = RequestMethod.GET)
    public String verify(@RequestParam("tz") String tz) throws AccessDeniedException {
        if (currentLogin == null || currentLogin.getUser() == null)
            throw new AccessDeniedException("Error");
        currentLogin.getUser().setTimezone(TimeZone.getTimeZone(tz));
        return new JSONObject().put("user", HelperUtil.squiggly("base", currentLogin.getUser())).
                put("accpr", HelperUtil.squiggly("base,project_detail", projectService.getViewableProjects())).
                put("g", currentLogin.getAuthoritiesGlobal().stream().map(a -> a.getAuthorityCode().name()).collect(Collectors.toList())).
                put("admin", currentLogin.getUser().isSuperAdmin()).put("tz", tz).toString();
    }

    @RequestMapping(value = "/auth/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request) throws JSONException {
        return authService.logout(request);
    }

    @RequestMapping(value = "/auth/setup", method = RequestMethod.GET)
    public String setupStatus() throws JSONException {
        return new JSONObject().put("status", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SETUP))
                .put("timezones", ZoneId.getAvailableZoneIds()).toString();
    }

    @RequestMapping(value = "/auth/setup", method = RequestMethod.POST)
    public String setup(@RequestBody String d) throws JSONException {
        JSONObject data = new JSONObject(d);
        if (!configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SETUP)) {
            Login user = new Login("Init", "setup@joindeskapp.com", "setup");
            user.setPassword(new BCryptPasswordEncoder().encode("admin"));
            user.setSuperAdmin(true);
            currentLogin.setUser(user);
            Login admin = loginRepo.findByEmailIgnoreCase(data.getString("email"));
            if (null == admin) {
                admin = userService.createUser("Pradeep K", data.getString("email"), data.getString("password"), true, true, false);
            }
            currentLogin.setUser(admin);
            currentLogin.setAuthorities(authService.getAuthorities(admin));
            configurationService.save(ConfigurationService.JDCONFIG.APP_TIMEZONE, ZoneId.of(data.getString("timezone")).getId());
            configurationService.save(ConfigurationService.JDCONFIG.APP_BUSINESS_START_TIME, data.getString("startTime"));
            configurationService.save(ConfigurationService.JDCONFIG.APP_BUSINESS_END_TIME, data.getString("endTime"));
            configurationService.save(ConfigurationService.JDCONFIG.APP_BASE_URL, data.getString("url"));
            configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SETUP, true);
            currentLogin.setUser(null);
            return new JSONObject().put("success", configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SETUP)).toString();
        } else {
            return new JSONObject().put("success", false)
                    .put("error", "Setup already completed").toString();
        }

    }
}