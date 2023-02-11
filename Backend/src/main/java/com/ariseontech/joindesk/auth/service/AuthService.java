package com.ariseontech.joindesk.auth.service;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.*;
import com.ariseontech.joindesk.auth.repo.AuthorityGlobalRepo;
import com.ariseontech.joindesk.auth.repo.AuthorityProjectRepo;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.repo.TokenRepo;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.event.service.EmailService;
import com.ariseontech.joindesk.project.domain.Group;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.repo.GlobalGroupRepo;
import com.ariseontech.joindesk.project.repo.GroupRepo;
import com.ariseontech.joindesk.project.service.ProjectService;
import com.ariseontech.joindesk.slack.SlackService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Log
public class AuthService {
    @Autowired
    private LoginRepo loginRepo;
    @Autowired
    private GroupRepo groupRepo;
    @Autowired
    private GlobalGroupRepo globalGroupRepo;
    @Autowired
    private TokenRepo tokenRepo;
    @Autowired
    private CurrentLogin currentLogin;
    @Autowired
    private AuthorityProjectRepo authorityProjectRepo;
    @Autowired
    private AuthorityGlobalRepo authorityGlobalRepo;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private IPWhiteBlackListingService ipWhiteBlackListingService;
    @Autowired
    private SlackService slackService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public String checkUser(String userName, PreferredAuthTypes mode) {
        JSONObject resp = new JSONObject();
        Login agent;
        if (userName.contains("@")) {
            agent = loginRepo.findByEmailIgnoreCase(userName.toLowerCase());
        } else {
            agent = loginRepo.findByUserNameIgnoreCase(userName.toLowerCase());
        }
        if (agent != null) {
            if (agent.isPendingActivation())
                return resp.put("success", false).put("error", "Account not activated, Please contact Administrator").toString();
            if (agent.isLocked())
                return resp.put("success", false).put("error", "Account locked, Please contact Administrator").toString();
            if (!agent.isActive())
                return resp.put("success", false).put("error", "Account deactivated").toString();
            Login user = new Login();
            user.setUserName(agent.getUserName());
            user.setPic(agent.getPic());
            user.setFullName(agent.getFullName());
            user.setMfaEnabled(agent.isMfaEnabled());
            user.setSlackEnabled(agent.isSlackEnabled());
            user.setPreferredAuth(agent.getPreferredAuth() != null ? agent.getPreferredAuth() : PreferredAuthTypes.EMAIL);
            if (ObjectUtils.isEmpty(agent.getToken()) || agent.getTokenExpiry() == null || agent.getTokenExpiry().isBefore(LocalDateTime.now())) {
                agent.setToken(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                agent.setTokenExpiry(LocalDateTime.now().plusMinutes(5));
                loginRepo.save(agent);
            }
            PreferredAuthTypes otpMode = mode != null ? mode : user.getPreferredAuth();
            switch (otpMode) {
                case EMAIL ->
                    //emailService.sendTextMail(agent.getEmail(), "OTP for login", "OTP: " + agent.getToken());
                        resp.put("info", "OTP sent via email");
                case SLACK -> {
                    if (!agent.isSlackEnabled())
                        resp.put("error", "Slack not available");
                    try {
                        slackService.postMessage(agent.getSlackID(), "OTP: " + agent.getToken(), null);
                    } catch (Exception e) {
                        resp.put("error", "Slack currently not available");
                        //emailService.sendTextMail(agent.getEmail(), "OTP for login", "OTP: " + agent.getToken());
                        resp.put("info", "OTP sent via email");
                        otpMode = PreferredAuthTypes.EMAIL;
                    }
                    resp.put("info", "OTP sent via slack");
                    otpMode = PreferredAuthTypes.SLACK;
                }
                case MFA -> {
                    if (!agent.isMfaEnabled()) {
                        resp.put("error", "Authenticator not configured");
                        //emailService.sendTextMail(agent.getEmail(), "OTP for login", "OTP: " + agent.getToken());
                        resp.put("info", "OTP sent via email");
                        otpMode = PreferredAuthTypes.EMAIL;
                    }
                    resp.put("info", "Enter TOTP from Authenticator");
                    otpMode = PreferredAuthTypes.MFA;
                }
            }
            return resp.put("success", true).put("otpMode", otpMode).put("user", HelperUtil.squiggly("base,user_detail", user)).toString();
        }
        return resp.put("success", false).put("error", "Invalid email").toString();
    }

    /*public String googleAuthenticate(String idtoken, String deviceFp, String deviceInfo, HttpServletRequest request, HttpServletResponse response) throws FirebaseAuthException {
        JSONObject resp = new JSONObject();
        System.out.println(idtoken);
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idtoken);
        Login agent = loginRepo.findByEmailIgnoreCase(token.getEmail().toLowerCase());
        if (agent != null) {
            // reset token
            agent.setToken(UUID.randomUUID().toString().substring(0, 8));
            agent.setTokenExpiry(LocalDateTime.now().minusMinutes(1));
            agent = loginRepo.save(agent);
            String currentIP = HelperUtil.getClientIp(request);
            if (!ipWhiteBlackListingService.isAllowedWeb(currentIP) || deviceInfo.isEmpty())
                return resp.put("success", false).put("message", "Access denied from current location").toString();
            if (!agent.isActive())
                return resp.put("success", false).put("message", "Account inactive, Please contact Administrator").toString();
            if (agent.isPendingActivation())
                return resp.put("success", false).put("message", "Account not activated, Please contact Administrator").toString();
            if (agent.isLocked())
                return resp.put("success", false).put("message", "Locked: " + agent.getLockReason()).toString();
            Map<String, Object> additionalClaims = new HashMap<>();
            additionalClaims.put("id", agent.getId());
            additionalClaims.put("name", agent.getFullName());
            String jwtToken = FirebaseAuth.getInstance().createCustomToken(agent.getUserName(), additionalClaims);
            Token t = new Token();
            t.setUser(agent);
            t.setToken(jwtToken);
            t.setUser(agent);
            t.setDeviceInfo(deviceInfo);
            t.setFp(deviceFp);
            t.setIp(currentIP);
            response.addHeader("X-AUTH-TOKEN", jwtToken);
            response.addHeader("Access-Control-Expose-Headers", "X-AUTH-TOKEN");
            tokenRepo.save(t);
            return resp.put("success", true).toString();
        }
        return resp.put("success", false).toString();
    }*/

    public String authenticate(String userName, String password, boolean remember, String deviceFp, String deviceInfo, PreferredAuthTypes mode, HttpServletRequest request, HttpServletResponse response) {
        JSONObject resp = new JSONObject();
        Login login;
        if (userName.contains("@")) {
            login = loginRepo.findByEmailIgnoreCase(userName.toLowerCase());
        } else {
            login = loginRepo.findByUserNameIgnoreCase(userName.toLowerCase());
        }
        if (login != null && new BCryptPasswordEncoder().matches(password, login.getPassword())) {
            //if (login != null && verifyMFAToken(login, password, mode)) {
            // reset token
            login.setToken(UUID.randomUUID().toString().substring(0, 8));
            login.setTokenExpiry(LocalDateTime.now().minusMinutes(1));
            login = loginRepo.save(login);
            String currentIP = HelperUtil.getClientIp(request);
            if (!ipWhiteBlackListingService.isAllowedWeb(currentIP) || deviceInfo.isEmpty())
                return resp.put("success", false).put("message", "Access denied from current location").toString();
            if (!login.isActive())
                return resp.put("success", false).put("message", "Account inactive, Please contact Administrator").toString();
            if (login.isPendingActivation())
                return resp.put("success", false).put("message", "Account not activated, Please contact Administrator").toString();
            if (login.isLocked())
                return resp.put("success", false).put("message", "Locked: " + login.getLockReason()).toString();
            String timeZone = request.getHeader("X-TZ");
            login.setTimezone(TimeZone.getTimeZone(timeZone));
            currentLogin.setUser(login);
            currentLogin.setAuthorities(getAuthorities(login));
            currentLogin.setAuthoritiesGlobal(getGlobalAuthorities(login));
            Map<String, Object> additionalClaims = new HashMap<>();
            additionalClaims.put("user", HelperUtil.squiggly("base,-email,-createdAt,-updatedAt,-pendingActivation," +
                    "-preferredAuth,-lockReason,-emailNotification,-slackNotification,-slackEnabled,-apiEnabled,-slackID,-slackAvailable,-qrUrl", login));
            additionalClaims.put("accpr", HelperUtil.squiggly("base,project_detail,-lead,-notifyViaSlack,-slackChannel," +
                    "-description,-timeTracking", projectService.getViewableProjects()));
            additionalClaims.put("g", getGlobalAuthorities(login).stream().map(a -> a.getAuthorityCode().name()).collect(Collectors.toList()));
            additionalClaims.put("admin", login.isSuperAdmin());
            //additionalClaims.put("deviceInfo", deviceInfo);
            //additionalClaims.put("deviceFp", deviceFp);
            additionalClaims.put("currentIP", currentIP);
            additionalClaims.put("timeZone", timeZone);
            additionalClaims.put("remember", remember ? "true" : "false");
            /*String jwtToken = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, appProperties.getJwtsecret()).compact();*/
            String jwtToken = jwtTokenUtil.generateToken(login.getUserName(), additionalClaims);
            Token t = new Token();
            t.setUser(login);
            t.setToken(jwtToken);
            t.setUser(login);
            t.setDeviceInfo(deviceInfo);
            t.setFp(deviceFp);
            t.setIp(currentIP);
            response.addHeader("X-AUTH-TOKEN", jwtToken);
            response.addHeader("Access-Control-Expose-Headers", "X-AUTH-TOKEN");
            tokenRepo.save(t);
            return resp.put("success", true).put("details", jwtToken).toString();
        }
        log.info("Incorrect login attempt by " + userName);
        return resp.put("success", false).toString();
    }

    public String forgot(Object reqq, HttpServletRequest request, HttpServletResponse response) {
        LoginRequestDTO req = (LoginRequestDTO) reqq;
        boolean success = true;
        boolean changed = false;
        String message = "Email sent with token if a matching account found";
        Login agent = loginRepo.findByEmailIgnoreCase(req.getUsername().toLowerCase());
        if (agent != null) {
            String newToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (ObjectUtils.isEmpty(req.getToken())) {
                if (agent.getTokenExpiry().minusMinutes(4).isAfter(LocalDateTime.now())) {
                    message = "Token sent recently try after some time";
                } else {
                    agent.setToken(newToken);
                    agent.setTokenExpiry(LocalDateTime.now().plusMinutes(5));
                    loginRepo.save(agent);
                    emailService.sendTextMail(agent.getEmail(), "Password reset request", "Token for reset: " + agent.getToken() + " . Token will be valid only for 5 minutes");
                }
            } else {
                if (ObjectUtils.isEmpty(req.getPassword()) || ObjectUtils.isEmpty(req.getConfirmPassword())
                        || req.getPassword().length() < 8 || req.getPassword().length() > 50) {
                    success = false;
                    message = "Invalid password";
                } else if (agent.getToken().equals(req.getToken()) && agent.getTokenExpiry().isAfter(LocalDateTime.now())) {
                    agent.setPassword(new BCryptPasswordEncoder().encode(req.getPassword()));
                    agent.setToken(newToken);
                    agent.setTokenExpiry(LocalDateTime.now().plusMinutes(5));
                    loginRepo.save(agent);
                    changed = true;
                    message = "Password changed successfully";
                } else {
                    success = false;
                    message = "Invalid or Expired token";
                }
            }
        }
        return new JSONObject().put("success", success).put("changed", changed).put("message", message).toString();
    }

    private boolean verifyMFAToken(Login agent, String password, PreferredAuthTypes mode) {
        if (mode.equals(PreferredAuthTypes.SLACK) || mode.equals(PreferredAuthTypes.EMAIL)) {
            return agent.getTokenExpiry().isAfter(LocalDateTime.now()) && agent.getToken().equals(password);
        } else if (agent.isMfaEnabled()) {
            try {
                return new GoogleAuthenticator().authorize(agent.getMfaKey(), Integer.parseInt(password));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @CacheEvict(value = "token", allEntries = true)
    public String logout(HttpServletRequest httpRequest) {
        String authToken = httpRequest.getHeader("X-AUTH-TOKEN");
        if (authToken != null && !authToken.isEmpty()) {
            Token token = tokenRepo.findByToken(authToken);
            if (null != token) tokenRepo.delete(token);
        }
        return new JSONObject().put("success", true).toString();
    }

    @Transactional
    @Cacheable(value = "token", key = "#authToken")
    public Token getToken(String authToken) {
        try {
            Token t = tokenRepo.findByToken(authToken);
            //Check token expiry
            jwtTokenUtil.getExpirationDateFromToken(authToken);
            t.setLastAccess(new Date());
            return tokenRepo.save(t);
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    @Cacheable(value = "authoritiesForLogin", key = "#l.id")
    public List<AuthorityProject> getAuthorities(Login l) {
        return authorityProjectRepo.findByLogin(l);
    }

    @Cacheable(value = "globalAuthoritiesForLogin", key = "#l.id")
    public List<AuthorityGlobal> getGlobalAuthorities(Login l) {
        return authorityGlobalRepo.findByLogin(l);
    }

    @Caching(evict = {
            @CacheEvict(value = "authoritiesForLogin", key = "#l.id"),
            @CacheEvict(value = "globalAuthoritiesForLogin", key = "#l.id")
    })
    public void calculateAuthoritiesForLogin(Login l) {
        //Project Level
        Set<AuthorityProject> authorityProjects = new HashSet<>();
        Set<Group> groups = groupRepo.findByUsersOrderByNameAsc(l);
        Set<Group> allUsersGroups = groupRepo.findByAllUsersTrueOrderByNameAsc();
        groups.addAll(allUsersGroups);
        groups.forEach(g -> g.getAuthorityCodes().forEach(c -> {
            AuthorityProject authorityProject = new AuthorityProject();
            authorityProject.setAuthorityCode(c);
            authorityProject.setLogin(l);
            authorityProject.setProject(g.getProject());
            authorityProjects.add(authorityProject);
        }));
        authorityProjectRepo.deleteByLogin(l);
        authorityProjectRepo.saveAll(authorityProjects);
        //Global
        Set<AuthorityGlobal> authorityGlobals = new HashSet<>();
        globalGroupRepo.findByUsersOrderByNameAsc(l).forEach(g -> g.getAuthorityCodes().forEach(c -> {
            AuthorityGlobal authorityGlobal = new AuthorityGlobal();
            authorityGlobal.setAuthorityCode(c);
            authorityGlobal.setLogin(l);
            authorityGlobals.add(authorityGlobal);
        }));
        authorityGlobalRepo.deleteByLogin(l);
        authorityGlobalRepo.saveAll(authorityGlobals);
    }

    /**
     * has global authority
     *
     * @param code
     * @return
     */
    public boolean hasGlobalAuthority(AuthorityCode code) {
        return isSuperAdmin() || currentLogin.getAuthoritiesGlobal().stream().anyMatch(ap -> ap.getAuthorityCode().equals(code));
    }

    /**
     * has Authority for any project
     *
     * @param code
     * @return
     */
    public boolean hasAuthority(AuthorityCode code) {
        return hasGlobalAuthority(code) || currentLogin.getAuthorities().stream().anyMatch(ap -> ap.getAuthorityCode().equals(code));
    }

    public boolean hasAnyAuthority(List<AuthorityCode> code) {
        AtomicBoolean match = new AtomicBoolean(false);
        currentLogin.getAuthorities().forEach(ap -> code.forEach(c -> {
            if (c.equals(ap)) match.set(true);
        }));
        currentLogin.getAuthoritiesGlobal().forEach(ap -> code.forEach(c -> {
            if (c.equals(ap)) match.set(true);
        }));
        return match.get();
    }

    public boolean hasAllAuthority(List<AuthorityCode> code) {
        Set<AuthorityCode> authorities = currentLogin.getAuthorities().stream().map(AuthorityProject::getAuthorityCode).collect(Collectors.toSet());
        authorities.addAll(currentLogin.getAuthoritiesGlobal().stream().map(AuthorityGlobal::getAuthorityCode).collect(Collectors.toSet()));
        return authorities.containsAll(code);
    }

    /**
     * has authority for project
     *
     * @param project
     * @param code
     * @return
     */
    public boolean hasAuthorityForProject(Project project, AuthorityCode code) {
        if (hasGlobalAuthority(code)) return true;
        return currentLogin.getAuthorities().stream().filter(ap -> ap.getAuthorityCode().equals(code)).anyMatch(ap -> ap.getProject().equals(project));
    }

    public List<AuthorityCode> getAllAuthorityForProject(Project p) {
        List<AuthorityCode> authorityList = currentLogin.getAuthorities().stream().filter(ap -> ap.getProject().equals(p)).map(AuthorityProject::getAuthorityCode).collect(Collectors.toList());
        authorityList.addAll(currentLogin.getAuthoritiesGlobal().stream().map(AuthorityGlobal::getAuthorityCode).collect(Collectors.toList()));
        return authorityList;
    }

    public Set<Project> getProjectsWithAuthority(AuthorityCode code) {
        if (currentLogin.getUser().isSuperAdmin() || hasGlobalAuthority(code)) {
            return new HashSet<>(projectService.getAllProjects());
        }
        Set<Project> projects = new HashSet<>();
        currentLogin.getAuthorities().stream().filter(ap -> ap.getAuthorityCode().equals(code)).forEach(ap -> projects.add(ap.getProject()));
        return projects;
    }

    public Set<Login> getProjectMembers(Project p) {
        return getProjectMembersbyAuthority(p, AuthorityCode.PROJECT_VIEW);
    }

    public Set<Login> getProjectMembersbyAuthority(Project p, AuthorityCode c) {
        Set<Login> members = authorityProjectRepo.findByProjectAndAuthorityCode(p, c).stream().map(AuthorityProject::getLogin).distinct().collect(Collectors.toSet());
        members.addAll(authorityGlobalRepo.findByAuthorityCode(c).stream().map(AuthorityGlobal::getLogin).distinct().collect(Collectors.toSet()));
        //if (isSuperAdmin()) members.add(currentLogin.getUser());
        return members.stream().filter(Login::isActive).collect(Collectors.toSet());
    }

    public Set<Login> getMembers() {
        return loginRepo.findByActiveTrue();
    }

    public Login getMemberByUserNameIgnoreCase(String username) {
        return loginRepo.findByUserNameIgnoreCase(username);
    }

    public Login getMemberByToken(String token) {
        return loginRepo.findByApiToken(token);
    }

    public boolean isSuperAdmin() {
        return currentLogin != null && currentLogin.getUser() != null && currentLogin.getUser().isSuperAdmin();
    }

    public Login currentLogin() {
        return currentLogin.getUser();
    }
}

