package com.ariseontech.joindesk;

import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.IssueFilterRepo;
import com.ariseontech.joindesk.issues.repo.ResolutionRepo;
import com.ariseontech.joindesk.issues.repo.WorkflowRepo;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.issues.service.IssueTypeService;
import com.ariseontech.joindesk.issues.service.RelationshipService;
import com.ariseontech.joindesk.issues.service.WorkflowService;
import com.ariseontech.joindesk.page.domain.Page;
import com.ariseontech.joindesk.page.service.PageService;
import com.ariseontech.joindesk.project.domain.GlobalGroup;
import com.ariseontech.joindesk.project.domain.Group;
import com.ariseontech.joindesk.project.domain.JDConfiguration;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.repo.GlobalGroupRepo;
import com.ariseontech.joindesk.project.repo.GroupRepo;
import com.ariseontech.joindesk.project.repo.JDConfigurationRepo;
import com.ariseontech.joindesk.project.repo.ProjectRepo;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.GroupService;
import com.ariseontech.joindesk.scheduler.service.JDSchedulerService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Log
class SetupInitializer {

    @Autowired
    private LoginRepo loginRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private GlobalGroupRepo globalGroupRepo;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private IssueTypeService issueTypeService;

    @Autowired
    private WorkflowRepo workflowRepo;

    @Autowired
    private CurrentLogin currentLogin;

    @Autowired
    private IssueService issueService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private JDConfigurationRepo jdConfigurationRepo;
    @Autowired
    private IssueFilterRepo issueFilterRepo;
    @Autowired
    private PageService pageService;
    @Autowired
    private JDSchedulerService jdSchedulerService;
    @Autowired
    private ConfigurationService configurationService;

    @Transactional(propagation = Propagation.REQUIRED)
    void setup() {
        if (loginRepo.count() <= 0) {
            configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SETUP, false);
        }
        //Create relationships
        if (relationshipService.count() == 0) {
            relationshipService.saveRelationship(new Relationship(null, "blocks", "blocks", "is blocked by"));
            relationshipService.saveRelationship(new Relationship(null, "duplicate", "duplicate", "is duplicated by"));
            relationshipService.saveRelationship(new Relationship(null, "related", "related", "is related to"));
        }
        //Create resolutions
        List<Resolution> resolutions = resolutionRepo.findAll();
        if (resolutions.isEmpty()) {
            resolutions.add(new Resolution("DONE"));
            resolutions.add(new Resolution("DUPLICATE"));
            resolutions.add(new Resolution("WITHDRAW"));
            resolutionRepo.saveAll(resolutions);
        }
        //Create configurations
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_TIMEZONE) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_TIMEZONE, ZoneId.of("Asia/Kolkata").getId()));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_BUSINESS_START_TIME) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_BUSINESS_START_TIME, "09:00"));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_BUSINESS_END_TIME) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_BUSINESS_END_TIME, "18:00"));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_EMAIL_ENABLED) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_EMAIL_ENABLED, false));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_EMAIL) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_EMAIL, true));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_ALLOWED_DOMAINS) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_ALLOWED_DOMAINS, ""));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_SLACK_ENABLED) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_SLACK_ENABLED, false));
        if (jdConfigurationRepo.findByKey(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_SLACK) == null)
            jdConfigurationRepo.save(new JDConfiguration(ConfigurationService.JDCONFIG.APP_SELF_REGISTRATION_REQUIRE_REVIEW_SLACK, true));

        //ReIndex Issues
        issueService.reindexAll();

        //Revoke after service complete
        currentLogin.setUser(null);

        //Schedule Jobs
        try {
            jdSchedulerService.scheduleDueDateJob();
            jdSchedulerService.getJobs().forEach(jobKey -> {
                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();
                log.info("[jobName] : " + jobName + " [groupName] : "
                        + jobGroup);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    @Transactional(propagation = Propagation.REQUIRED)
    void setup2() {
        Login user = new Login("Init", "prdp.dev@gmail.com", "admin");
        user.setPassword(new BCryptPasswordEncoder().encode("admin"));
        user.setSuperAdmin(true);
        currentLogin.setUser(user);
        Login admin = loginRepo.findByEmailIgnoreCase("prdp.dev@gmail.com");
        if (null == admin) {
            admin = userService.createUser("Pradeep K", "prdp.dev@gmail.com", HelperUtil.getUniqueID(), true, true, false);
        }
        currentLogin.setUser(admin);
        currentLogin.setAuthorities(authService.getAuthorities(admin));
        //Create project
        List<Project> projects = projectRepo.findAll();
        Project project;
        if (projects.isEmpty()) {
            project = new Project("SP", "Sample Project");
            admin = loginRepo.findByEmailIgnoreCase("prdp.dev@gmail.com");
            project.setLead(admin);
            projectRepo.save(project);
            try {
                Page p = new Page();
                p.setTitle("Getting Started");
                p.setContent("# Heading 1\\n## Heading 2\\n### Heading 3\\n#### Heading 4\\n##### Heading 5\\n###### Heading 6\\n\\n### Blockquotes\\n> Blockquotes\\n\\n### Inline codes\\n    <?php\\n        echo \\\"Hello world!\\\";\\n    ?>\\n\\n### Lists\\n\\n#### Unordered Lists\\n\\n- Item 1\\n- Item 2\\n- Item 3\\n\\t- Item 3.1\\n- Item 4\\n\\n####  Ordered Lists\\n\\n1. Item 1\\n2. Item 2\\n3. Item 3\\n----\\n### Tables\\n\\nFirst Header  | Second Header\\n------------- | -------------\\nContent Cell  | Content Cell\\nContent Cell  | Content Cell \"\n");
                pageService.savePage("SP", p);
            } catch (Exception ignored) {
            }
        } else {
            project = projectRepo.findAll().get(0);
        }
        //Create Global Manager group
        Set<GlobalGroup> globalManagerGroup = globalGroupRepo.findByNameOrderByNameAsc("Manager");
        if (null == globalManagerGroup) {
            GlobalGroup adminGroup = new GlobalGroup("Manager");
            Set<Login> users = adminGroup.getUsers();
            users.add(admin);
            adminGroup.setUsers(users);
            adminGroup.setAuthorityCodes(EnumSet.allOf(AuthorityCode.class));
            groupService.saveGlobalGroup(adminGroup);
        }
        //Create Manager group
        Set<Group> adminGroups = groupRepo.findByNameOrderByNameAsc("Manger");
        if (null == adminGroups) {
            Group adminGroup = new Group("Manager");
            adminGroup.setProject(project);
            Set<Login> users = adminGroup.getUsers();
            users.add(admin);
            adminGroup.setUsers(users);
            adminGroup.setAuthorityCodes(EnumSet.allOf(AuthorityCode.class));
            groupService.createGroup(project.getKey(), adminGroup);
        }
        //Create Developer group for base project q
        Set<Group> developerGroups = groupRepo.findByNameOrderByNameAsc("Developers");
        if (developerGroups.isEmpty()) {
            Group developerGroup = new Group("Developers");
            developerGroup.setProject(project);
            EnumSet<AuthorityCode> authorities = EnumSet.noneOf(AuthorityCode.class);
            authorities.add(AuthorityCode.ATTACHMENT_CREATE);
            authorities.add(AuthorityCode.ATTACHMENT_DELETE_OWN);
            authorities.add(AuthorityCode.ISSUE_CREATE);
            authorities.add(AuthorityCode.ISSUE_EDIT);
            authorities.add(AuthorityCode.ISSUE_LINK);
            authorities.add(AuthorityCode.ISSUE_TRANSITION);
            authorities.add(AuthorityCode.COMMENT_ADD);
            authorities.add(AuthorityCode.COMMENT_EDIT_OWN);
            authorities.add(AuthorityCode.COMMENT_DELETE_OWN);
            authorities.add(AuthorityCode.PROJECT_VIEW);
            authorities.add(AuthorityCode.WIKI_VIEW);
            authorities.add(AuthorityCode.WIKI_EDIT);
            developerGroup.setAuthorityCodes(authorities);
            developerGroup.setProject(project);
            Set<Login> users = developerGroup.getUsers();
            users.add(admin);
            developerGroup.setUsers(users);
            groupService.createGroup(project.getKey(), developerGroup);
        }
        currentLogin.setAuthorities(authService.getAuthorities(admin));
        //Create Workflow
        Workflow workflow = workflowRepo.findByName("Incident Management");
        if (null == workflow) {
            //Create one
            workflow = workflowService.create(new Workflow("Incident Management", project));
        }
        IssueType issueType = issueTypeService.findByNameAndProject("Incident", project);
        if (issueType == null) {
            IssueType it = new IssueType("Incident", "flag", "Incident Issue", project);
            it.setWorkflow(workflow);
            issueType = issueTypeService.create(project.getKey(), it);
            issueType = issueTypeService.changeWorkflow(issueType.getId(), workflow, new WorkflowChange());
        }
        //Create sample issue
        Set<Issue> issues = issueService.findByProject(project.getId());
        if (issues.isEmpty()) {
            Issue issue = new Issue(issueType, "Sample Issue", "Sample Issue desc", project, admin, Priority.Normal);
            issueService.create(issue, project.getId());
        }
        //Create default filters
        IssueFilter issueFilter = new IssueFilter("Open", admin, "Updated", "DESC", null, true, project);
        ArrayList<IssueSearchQueryRule> rules = new ArrayList<>();
        rules.add(new IssueSearchQueryRule("resolution", "IN", new String[]{"0"}));
        issueFilter.setQuery(new IssueSearchQuery("and", rules));
        IssueFilter issueFilter2 = new IssueFilter("Resolved", admin, "Updated", "DESC", null, true, project);
        rules = new ArrayList<>();
        rules.add(new IssueSearchQueryRule("resolution", "NOT IN", new String[]{"unresolved"}));
        issueFilter2.setQuery(new IssueSearchQuery("and", rules));
        List<IssueFilter> existingFilters = issueFilterRepo.findByOpenTrue();
        if (existingFilters.isEmpty()) {
            issueFilterRepo.save(issueFilter);
            issueFilterRepo.save(issueFilter2);
        }
    }

}
