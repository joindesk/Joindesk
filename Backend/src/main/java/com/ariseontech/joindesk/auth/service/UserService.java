package com.ariseontech.joindesk.auth.service;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.domain.PreferredAuthTypes;
import com.ariseontech.joindesk.auth.domain.SlackUser;
import com.ariseontech.joindesk.auth.domain.Token;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.repo.TokenRepo;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.event.service.EmailService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.repo.IssueFilterRepo;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.ProjectService;
import com.ariseontech.joindesk.slack.SlackService;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log
public class UserService {

    @Autowired
    private LoginRepo loginRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CurrentLogin currentLogin;
    @Autowired
    private EmailService emailService;
    @Autowired
    private TokenRepo tokenRepo;
    @Value("${upload-dir}")
    private String uploadPath;
    @Autowired
    private HelperUtil helperUtil;
    @Autowired
    private SlackService slackService;
    @Autowired
    private IssueFilterRepo issueFilterRepo;

    private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int type) {

        BufferedImage resizedImage = new BufferedImage(100, 0b1100100, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, 100, 100, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        return resizedImage;
    }

    @CacheEvict(value = "members", allEntries = true)
    public Login createUser(String fullName, String email, String password, boolean superAdmin, boolean active, boolean pendingActivation) {
        if (!pendingActivation)
            checkManageAccess();
        String userName = email.substring(0, email.indexOf("@"));
        if (userName.equalsIgnoreCase("unassigned"))
            throw new JDException("Invalid username", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        if (null != loginRepo.findByUserNameIgnoreCase(userName.toLowerCase())) {
            throw new JDException("Username already taken", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (null != loginRepo.findByEmailIgnoreCase(email.toLowerCase())) {
            throw new JDException("Email already taken", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (pendingActivation) {
            String allowedDomains = configurationService.getString(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_ALLOWED_DOMAINS).trim();
            if (!ObjectUtils.isEmpty(allowedDomains)) {
                List<String> allowed = Arrays.asList(allowedDomains.split(","));
                if (!allowed.isEmpty() && !allowed.contains(email.toLowerCase().substring(email.indexOf("@"))))
                    throw new JDException("Email not allowed for signup", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
            }
        }
        Login user = new Login(fullName, email.toLowerCase(), userName.toLowerCase());
        user.setSuperAdmin(superAdmin);
        user.setActive(active);
        user.setPendingActivation(pendingActivation);
        user.setApiToken(HelperUtil.generateApiToken(user.getUserName()));
        if (!active) user.setFullName(user.getFullName() + " (Inactive)");
        final GoogleAuthenticatorKey key = new GoogleAuthenticator().createCredentials();
        URIBuilder uri = (new URIBuilder()).setScheme("otpauth").setHost("totp").setPath("/JD (" + user.getUserName() + ")").setParameter("secret", key.getKey());
        user.setQrUrl(uri.toString());
        user.setMfaKey(key.getKey());
        try {
            user.setPassword(new BCryptPasswordEncoder().encode(password));
            loginRepo.save(user);
        } catch (Exception e) {
            throw new JDException(e.getMessage(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (active)
            emailService.sendTextMail(email, "Welcome to Joindesk", "Your account is activated, start using Joindesk at " + configurationService.getApplicationDomain());
        //Create filter
        /*IssueFilter issueFilter = new IssueFilter("My Open", user, "Updated", "DESC", null, false);
        ArrayList<IssueSearchQueryRule> rules = new ArrayList<>();
        rules.add(new IssueSearchQueryRule("assignee", "IN", new String[]{user.getId().toString()}));
        issueFilter.setQuery(new IssueSearchQuery("and", rules));
        issueFilterRepo.save(issueFilter);*/
        return user;
    }

    public String registerUser(String fullName, String email) {
        if (configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_EMAIL_ENABLED)) {
            boolean pending = configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_EMAIL);
            createUser(fullName, email, HelperUtil.getUniqueID(), false, false, pending);
            emailService.sendTextMail(email, "Welcome to Joindesk", pending ? "Your account is under review, you will receive an confirmation once account is reviewed"
                    : "Your account is activated, you can start using Joindesk");
            if (pending) {
                loginRepo.findBySuperAdminTrue().forEach(l -> emailService.sendTextMail(l.getEmail(), "Review request for user " + fullName, "New user registration awaits your review"));
            }
            return new JSONObject().put("success", true).put("pending", pending).toString();
        }
        return new JSONObject().put("success", false).toString();
    }

    public List<Login> getAll() {
        checkManageAccess();
        return loginRepo.findAllByOrderByEmailAsc().stream().filter(l -> !l.isPendingActivation()).collect(Collectors.toList());
    }

    public List<Login> getAllPending() {
        checkManageAccess();
        return loginRepo.findAllByOrderByEmailAsc().stream().filter(Login::isPendingActivation).collect(Collectors.toList());
    }

    //    @Cacheable(value = "members", key = "'memberinternal-' + #loginID")
    public Login getInternal(Long loginID) {
        Optional<Login> login = loginRepo.findById(loginID);
        if (login.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        return login.get();
    }

    public Login get(Long loginID) {
        if (!currentLogin.getUser().getId().equals(loginID)) {
            checkManageAccess();
        }
        Optional<Login> login = loginRepo.findById(loginID);
        if (login.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        Login l = login.get();
        l.setEditable(hasManageAccess());
        l.setSlackAvailable(slackService.isSlackEnabled());
        //Prepare MFA if not already enabled
        if (!l.isMfaEnabled() || ObjectUtils.isEmpty(l.getMfaKey())) {
            final GoogleAuthenticatorKey key = new GoogleAuthenticator().createCredentials();
            URIBuilder uri = (new URIBuilder()).setScheme("otpauth").setHost("totp").setPath("/JD (" + l.getUserName() + ")").setParameter("secret", key.getKey());
            l.setQrUrl(uri.toString());
            l.setMfaKey(key.getKey());
            loginRepo.save(l);
        }
        if (l.getPreferredAuth() == null)
            l.setPreferredAuth(PreferredAuthTypes.EMAIL);
        return l;
    }

    public void removePic(Long id) {
        if (currentLogin.getUser().getId().equals(id)) {
            loginRepo.findById(id).ifPresent(this::removePic);
        }
    }

    public Login getUser(Long loginID) {
        return find(loginID);
    }

    //    @Cacheable(value = "members", key = "'memberByslackID-' + #slackUserID")
    public Login getSlackUser(String slackUserID) {
        Optional<Login> login = Optional.ofNullable(loginRepo.findBySlackID(slackUserID));
        if (login.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        return login.get();
    }

    //    @Cacheable(value = "members", key = "'memberByLoginID-' + #loginID")
    public Login find(Long loginID) {
        Optional<Login> login = loginRepo.findById(loginID);
        if (login.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        login.get().setEditable(hasManageAccess());
        return login.get();
    }

    //    @Cacheable(value = "members", key = "'memberByEmail-' + #email")
    public Login findbyEmail(String email) {
        Optional<Login> login = Optional.ofNullable(loginRepo.findByEmailIgnoreCase(email));
        if (login.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        login.get().setEditable(hasManageAccess());
        return login.get();
    }

    private boolean hasManageAccess() {
        return authService.isSuperAdmin();
    }

    private void checkManageAccess() {
        if (!hasManageAccess()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    @CacheEvict(value = "members", allEntries = true)
    public Login save(Long id, String fullName, String email, boolean superAdmin, boolean emailN, boolean slackN) {
        if (!id.equals(currentLogin.getUser().getId())) {
            checkManageAccess();
        }
        Login login = get(id);
        if (login.isPendingActivation())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (!ObjectUtils.isEmpty(fullName))
            login.setFullName(fullName);
        if (!ObjectUtils.isEmpty(email))
            login.setEmail(email);
        login.setEmailNotification(emailN);
        login.setSlackNotification(slackN);
        login.setSuperAdmin(superAdmin);
        if (login.getPreferredAuth() == null)
            login.setPreferredAuth(PreferredAuthTypes.EMAIL);
        currentLogin.setUser(login);
        return loginRepo.save(login);
    }

    @Caching(evict = {
            @CacheEvict(value = "members", allEntries = true),
            @CacheEvict(value = "token", allEntries = true),
    })
    public Login saveFromAdmin(Login l) {
        checkManageAccess();
        Login login = get(l.getId());
        if (login.isPendingActivation())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (!ObjectUtils.isEmpty(l.getFullName()))
            login.setFullName(l.getFullName());
        if (!ObjectUtils.isEmpty(l.getEmail()))
            login.setEmail(l.getEmail());
        login.setSuperAdmin(l.isSuperAdmin());
        if (login.isLocked() != l.isLocked()) {
            login.setLocked(l.isLocked());
            login.setLockReason(l.isLocked() ? l.getLockReason() : "");
        }
        if (login.isApiEnabled() != l.isApiEnabled()) {
            login.setApiEnabled(l.isApiEnabled());
            if (login.getApiToken().isEmpty())
                login.setApiToken(HelperUtil.generateApiToken(login.getUserName()));
        }
        if (login.isActive() != l.isActive()) {
            login.setActive(l.isActive());
            if (!login.isActive()) {
                login.setFullName(login.getFullName() + " (Inactive)");
            } else {
                if (login.getFullName().contains("(Inactive)"))
                    login.setFullName(login.getFullName().substring(0, login.getFullName().indexOf("(Inactive)")));
            }
        }
        if (!ObjectUtils.isEmpty(l.getPassword()))
            login.setPassword(l.getPassword());
        if (login.getPreferredAuth() == null)
            login.setPreferredAuth(PreferredAuthTypes.EMAIL);
        //clear all login`s for user if not active or locked
        if (login.isLocked() || !login.isActive()) {
            tokenRepo.deleteInBatch(tokenRepo.findAllByUser(login));
        }
        currentLogin.setUser(login);

        return loginRepo.save(login);
    }

    public String approvePending(Login l) {
        checkManageAccess();
        Login login = get(l.getId());
        login.setActive(true);
        login.setPendingActivation(false);
        if (login.getFullName().contains("(Inactive)"))
            login.setFullName(login.getFullName().substring(0, login.getFullName().indexOf("(Inactive)")));
        loginRepo.save(login);
        emailService.sendTextMail(login.getEmail(), "Account activated : Joindesk", "Your account is activated, you can start using joindesk");
        return new JSONObject().put("success", true).toString();
    }

    public String rejectPending(Login l) {
        checkManageAccess();
        Login login = get(l.getId());
        String email = login.getEmail();
        issueFilterRepo.deleteAll(issueFilterRepo.findByOwner(login));
        loginRepo.delete(login);
        emailService.sendTextMail(email, "Registration request rejected : Joindesk", "Your registration request is rejected, Please contact administrator");
        return new JSONObject().put("success", true).toString();
    }

    public String getApiToken(Login l) {
        checkManageAccess();
        Login login = get(l.getId());
        if (ObjectUtils.isEmpty(login.getApiToken())) {
            login.setApiToken(HelperUtil.generateApiToken(login.getUserName()));
            login = loginRepo.save(login);
        }
        return login.getApiToken();
    }

    public String resetApiToken(Login l) {
        checkManageAccess();
        Login login = get(l.getId());
        login.setApiToken(HelperUtil.generateApiToken(login.getUserName()));
        login = loginRepo.save(login);
        return login.getApiToken();
    }

    private void removePic(Login user) {
        user.setPic(null);
        loginRepo.save(user);
    }

    public Login savePic(MultipartFile file, Long id) throws IOException {
        if (!authService.currentLogin().getId().equals(id))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Login login = get(id);

        // Get the file and save it somewhere
        byte[] bytes = file.getBytes();
        String name = login.getUserName() + "" + new Date().getTime();
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        String originalPathName = helperUtil.getDataPath(uploadPath) + name + "." + ext;
        String resizedPathName = name + "_100." + ext;
        Path path = Paths.get(originalPathName);
        Files.write(path, bytes);

        File file2 = new File(originalPathName);
        Thumbnails.of(file2).size(100, 100).toFile(new File(helperUtil.getDataPath(uploadPath) + resizedPathName));

        if (!ObjectUtils.isEmpty(login.getPic())) {
            FileUtils.forceDelete(new File(helperUtil.getDataPath(uploadPath) + login.getPic()));
        }
        login.setPic(resizedPathName);
        loginRepo.save(login);
        FileUtils.forceDelete(new File(originalPathName));
        currentLogin.setUser(login);
        return login;
    }

    public Set<Token> getActiveSessions(Long id) {
        if (!authService.currentLogin().getId().equals(id))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Login login = get(id);
        Set<Token> tokens = new HashSet<>();
        tokenRepo.findAllByUser(login).forEach(t -> {
            if (t.getToken().equals(currentLogin.getToken()))
                t.setCurrent(true);
            Map<String, String> device = new HashMap<>();
            JSONObject jo = new JSONObject(new String(Base64.getDecoder().decode(t.getDeviceInfo())));
            if (jo.has("os")) device.put("os", jo.getString("os"));
            if (jo.has("browser")) device.put("browser", jo.getString("browser"));
            if (jo.has("device")) device.put("device", jo.getString("device"));
            if (jo.has("os_version")) device.put("os_version", jo.getString("os_version"));
            if (jo.has("browser_version")) device.put("browser_version", jo.getString("browser_version"));
            t.setDevice(device);
            tokens.add(t);
        });
        return tokens;
    }

    @CacheEvict(value = "token", allEntries = true)
    public void removeActiveSession(Token t) {
        tokenRepo.findAllByUser(currentLogin.getUser()).stream().filter(to -> t.getId().equals(to.getId())).findAny().ifPresent(to -> tokenRepo.delete(to));
    }

    @CacheEvict(value = "token", allEntries = true)
    public void removeAllActiveSessions() {
        //Remove all tokens except current
        tokenRepo.findAllByUser(currentLogin.getUser()).stream().filter(to -> !to.getToken().equals(currentLogin.getToken())).forEach(t -> tokenRepo.delete(t));
    }

    public void requestSlackConnect(String slackUsername) throws IOException, SlackApiException {
        Login login = get(authService.currentLogin().getId());
        UsersListResponse resp = slackService.getUsers();
        if (!resp.isOk()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<User> matchedUser = resp.getMembers().stream().filter(m -> (m.getProfile().getEmail() != null && m.getProfile().getEmail().equalsIgnoreCase(slackUsername)) || m.getName().equals(slackUsername)).findAny();
        if (matchedUser.isPresent()) {
            if (ObjectUtils.isEmpty(login.getSlackToken()))
                login.setSlackToken(String.valueOf(new Date().getTime()).substring(0, 6));
            loginRepo.save(login);
            slackService.postMessage(matchedUser.get().getId(), "OTP to connect: " + login.getSlackToken(), null);
        } else {
            throw new JDException("No matching slack user", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    public void slackConnect(String slackUsername, String token) throws IOException, SlackApiException {
        Login login = get(authService.currentLogin().getId());
        UsersListResponse resp = slackService.getUsers();
        if (!resp.isOk()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<User> matchedUser = resp.getMembers().stream().filter(m -> (m.getProfile().getEmail() != null && m.getProfile().getEmail().equalsIgnoreCase(slackUsername)) || m.getName().equals(slackUsername)).findAny();
        if (matchedUser.isPresent() && !ObjectUtils.isEmpty(login.getSlackToken()) && login.getSlackToken().equals(token)) {
            login.setSlackEnabled(true);
            login.setSlackToken(String.valueOf(new Date().getTime()).substring(0, 6));
            login.setSlackID(matchedUser.get().getId());
            if (login.isMfaEnabled() && login.getPreferredAuth().equals(PreferredAuthTypes.EMAIL)) {
                login.setPreferredAuth(PreferredAuthTypes.SLACK);
                login.setEmailNotification(false);
            }
            loginRepo.save(login);
            slackService.postMessage(matchedUser.get().getId(), "Connected to slack", null);
        } else {
            throw new JDException("No matching slack user", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    public void slackDisconnect() {
        Login login = get(authService.currentLogin().getId());
        login.setSlackEnabled(false);
        login.setSlackToken(null);
        login.setSlackID(null);
        login.setPreferredAuth(login.isMfaEnabled() ? PreferredAuthTypes.MFA : PreferredAuthTypes.EMAIL);
        loginRepo.save(login);
    }

    public boolean slackStatus() {
        return slackService.isSlackEnabled();
    }

    public List<SlackUser> fetchSlackUsers() throws IOException, SlackApiException {
        checkManageAccess();
        Set<String> emails = loginRepo.getEmails();
        return slackService.getUsers().getMembers().stream().
                filter(u -> !u.getName().equals("slackbot") && !u.isBot() && !u.isDeleted() && !u.isRestricted()).
                filter(u -> !emails.contains(u.getProfile().getEmail())).
                map(u -> new SlackUser(u.getId(), u.getProfile().getDisplayName() + "(" + u.getName() + ")", "")).collect(Collectors.toList());
    }

    @CacheEvict(value = "members", allEntries = true)
    public void importSlackUser(String id) throws IOException, SlackApiException {
        checkManageAccess();
        User user = slackService.getUser(id).getUser();
        String fullName = user.getProfile().getDisplayName();
        if (ObjectUtils.isEmpty(fullName)) fullName = user.getName();
        Login login = createUser(fullName, user.getProfile().getEmail(), HelperUtil.getUniqueID(), false, true, false);
        if (login != null) {
            if (null != TimeZone.getTimeZone(user.getTz()))
                login.setTimezone(TimeZone.getTimeZone(user.getTz()));
            login.setSlackEnabled(true);
            login.setSlackID(user.getId());
            loginRepo.save(login);
        }
    }

    public List<Login> searchByProject(String projectKey, String q) {
        Project p = projectService.findByKey(projectKey);
        if (null == p)
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectViewAccess(p))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return projectService.getMembersByProjectKey(projectKey).stream()
                .filter(n -> (n.getFullName() + " " + n.getUserName()).toLowerCase().contains(q.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void activateMfa(int token) {
        Optional<Login> l = loginRepo.findById(currentLogin.getUser().getId());
        if (l.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        Login login = l.get();
        if (login.isMfaEnabled())
            throw new JDException("MFA already enabled", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        System.out.println(login.getMfaKey());
        System.out.println(token);
        System.out.println(new GoogleAuthenticator().getTotpPassword(login.getMfaKey()));
        if (new GoogleAuthenticator().authorize(login.getMfaKey(), token)) {
            login.setMfaEnabled(true);
            login.setPreferredAuth(PreferredAuthTypes.MFA);
            loginRepo.save(login);
        } else
            throw new JDException("Invalid / Expired code, please try again", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
    }

    public void setPreferredAuth(PreferredAuthTypes type) {
        Login login = get(authService.currentLogin().getId());
        if (type.equals(PreferredAuthTypes.SLACK) && !login.isSlackEnabled())
            throw new JDException("Slack is not active", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        if (type.equals(PreferredAuthTypes.MFA) && !login.isMfaEnabled())
            throw new JDException("Authenticator is not active", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        login.setPreferredAuth(type);
        loginRepo.save(login);
    }

    public void deactivateMFA() {
        Optional<Login> l = loginRepo.findById(currentLogin.getUser().getId());
        if (l.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        Login login = l.get();
        if (!login.isMfaEnabled())
            throw new JDException("MFA already inactive", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        login.setMfaEnabled(false);
        login.setPreferredAuth(login.isSlackEnabled() ? PreferredAuthTypes.SLACK : PreferredAuthTypes.EMAIL);
        //Reset MFA Key
        final GoogleAuthenticatorKey key = new GoogleAuthenticator().createCredentials();
        URIBuilder uri = (new URIBuilder()).setScheme("otpauth").setHost("totp").setPath("/JD (" + login.getUserName() + ")").setParameter("secret", key.getKey());
        login.setQrUrl(uri.toString());
        login.setMfaKey(key.getKey());
        loginRepo.save(login);
    }

    public String changePassword(String old_password, String password, String confirm_password) {
        JSONObject data = new JSONObject();
        boolean success = false;
        String error = "";
        Optional<Login> l = loginRepo.findById(currentLogin.getUser().getId());
        if (l.isEmpty()) throw new JDException("", ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        Login login = l.get();
        if (new BCryptPasswordEncoder().matches(old_password, login.getPassword())) {
            if (password.equals(confirm_password)) {
                login.setPassword(new BCryptPasswordEncoder().encode(password));
                loginRepo.save(login);
                success = true;
            } else {
                error = "Password and confirm password are not matching";
            }
        } else if (password.equalsIgnoreCase(old_password)) {
            error = "Old and new password cannot be the same";
        } else {
            error = "Old password is invalid";
        }
        return data.put("success", success).put("error", error).toString();
    }
}
