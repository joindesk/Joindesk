package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.board.domain.Lane;
import com.ariseontech.joindesk.board.service.BoardService;
import com.ariseontech.joindesk.event.domain.IssueEvent;
import com.ariseontech.joindesk.event.domain.IssueEventHandler;
import com.ariseontech.joindesk.event.domain.IssueEventType;
import com.ariseontech.joindesk.event.domain.JDEventHandler;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.exception.JDTransitionException;
import com.ariseontech.joindesk.git.repo.GitBranchRepo;
import com.ariseontech.joindesk.git.repo.GitCommitRepo;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.*;
import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.domain.TimeTracking;
import com.ariseontech.joindesk.project.repo.CustomFieldRepo;
import com.ariseontech.joindesk.project.repo.GroupRepo;
import com.ariseontech.joindesk.project.repo.ProjectRepo;
import com.ariseontech.joindesk.project.service.ComponentService;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.LabelService;
import com.ariseontech.joindesk.project.service.ProjectService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Log
public class IssueService {

    @Value("${attachment-dir}")
    private String uploadPath;
    @Autowired
    private IssueTypeRepo issueTypeRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private BoardService boardService;
    @Autowired
    private AttachmentRepo attachmentRepo;
    @Autowired
    private CommentRepo commentRepo;
    @Autowired
    private TaskRepo taskRepo;
    @Autowired
    private UserService userService;
    @Autowired
    private IssueRepo issueRepo;
    @Autowired
    private IssueCustomRepo issueCustomRepo;
    @Autowired
    private IssueSearchCustomRepo issueSearchCustomRepo;
    @Autowired
    private Validator validator;
    @Autowired
    private ProjectRepo projectRepo;
    @Autowired
    private CurrentLogin currentLogin;
    @Autowired
    private IssueFilteringRepo issueFilteringRepo;
    @Autowired
    private CustomFieldRepo customFieldRepo;
    @Autowired
    private IssueTypeService issueTypeService;
    @Autowired
    private IssueHistoryRepo issueHistoryRepo;
    @Autowired
    private ResolutionService resolutionService;
    @Autowired
    private IssueEventHandler issueEventService;
    @Autowired
    private JDEventHandler jdEventService;
    @Autowired
    private WatchersRepo watchersRepo;
    @Autowired
    private GroupRepo groupRepo;
    @Autowired
    private WorkLogService workLogService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private ComponentService componentService;
    @Autowired
    private LabelRepo labelRepo;
    @Autowired
    private LabelService labelService;
    @Autowired
    private IssueFilterRepo issueFilterRepo;
    @Autowired
    private IssueViewRepo issueViewRepo;
    @Autowired
    private GitBranchRepo gitBranchRepo;
    @Autowired
    private GitCommitRepo gitCommitRepo;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HelperUtil helperUtil;
    @Autowired
    private IssueAsyncService issueAsyncService;

    public Set<Issue> findAll(IssueFilterDTO filter) {
        String f = filter.getContainsText();
        //TODO: check using regex
        Set<Issue> result = new HashSet<>();
        if (f.contains("-")) {
            Issue i = get(getProjectKeyFromPair(f), getIssueKeyFromPair(f));
            if (null != i) {
                result.add(i);
            }
            result = issueRepo.containsText("%" + f + "%", "%" + f + "%");
        } else {
            result = issueRepo.containsText("%" + f + "%", "%" + f + "%");
        }
        return result.stream().filter(r -> canView(r.getProject())).collect(Collectors.toSet());
    }

    public IssueFilterDTO getBaseFilter(String projectKey, Long filterId) {
        IssueFilterDTO filter = new IssueFilterDTO();

        if (filterId > 0) {
            Optional<IssueFilter> f = issueFilterRepo.findById(filterId);
            if (f.isPresent() && f.get().getProject().getKey().equalsIgnoreCase(projectKey) && (f.get().isOpen()
                    || f.get().getOwner().equals(currentLogin.getUser()))) {
                f.get().setReadonly(!f.get().getOwner().equals(currentLogin.getUser()));
                filter.setFilter(f.get());
            }
        } else {
            //set default filter with open only
            IssueFilter f = new IssueFilter();
//            IssueSearchQueryRule rule = new IssueSearchQueryRule("resolution", "IN", new String[]{"0"});
//            IssueSearchQuery isq = new IssueSearchQuery("and", new ArrayList<>(Arrays.asList(rule)));
//            f.setQuery(isq);
            filter.setFilter(f);
        }
        filter.setPossibleProjects(authService.getProjectsWithAuthority(AuthorityCode.PROJECT_VIEW));
        if (!ObjectUtils.isEmpty(projectKey)) {
            Project project = projectRepo.findByKey(projectKey);
            if (null == project) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
            if (!canView(project))
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            //if (filter.getPossibleProjects().contains(project))
            filter.setProjects(Collections.singleton(project));
        }
        filter = getPossibleOptions(filter);
        //Issue Types
        IssueFilterGroup issueTypeGroup = new IssueFilterGroup("issue_type", "Issue Type", "multiselect");
        issueTypeGroup.setOptions(filter.getPossibleIssueTypes().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getName())).collect(Collectors.toList()));
        filter.filters.add(issueTypeGroup);
        //Label
        IssueFilterGroup labelGroup = new IssueFilterGroup("label", "Label", "multiselect");
        labelGroup.setOptions(filter.getPossibleLabels().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getName())).collect(Collectors.toList()));
        filter.filters.add(labelGroup);
        //Component
        IssueFilterGroup componentGroup = new IssueFilterGroup("component", "Component", "multiselect");
        componentGroup.setOptions(filter.getPossibleComponents().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getName())).collect(Collectors.toList()));
        filter.filters.add(componentGroup);
        //Resolution
        IssueFilterGroup res = new IssueFilterGroup("resolution", "Resolution", "multiselect");
        res.setOptions(filter.getPossibleResolutions().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getName())).collect(Collectors.toList()));
        filter.filters.add(res);
        //Version
        IssueFilterGroup version = new IssueFilterGroup("version", "Version", "multiselect");
        version.setOptions(filter.getPossibleVersions().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getName())).collect(Collectors.toList()));
        filter.filters.add(version);
        //Members
        IssueFilterGroup members = new IssueFilterGroup("assignee", "Assignee", "multiselect");
        members.setOptions(filter.getPossibleMembers().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getFullName())).collect(Collectors.toList()));
        filter.filters.add(members);
        members = new IssueFilterGroup("reporter", "Reporter", "multiselect");
        members.setOptions(filter.getPossibleMembers().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getFullName())).collect(Collectors.toList()));
        filter.filters.add(members);
        //Statuses
        IssueFilterGroup statuses = new IssueFilterGroup("current_step", "Status", "multiselect");
        statuses.setOptions(filter.getPossibleStatus().stream().map(it -> new IssueFilterOptions(it.getId().toString(), it.getName())).collect(Collectors.toList()));
        filter.filters.add(statuses);
        //Priority
        IssueFilterGroup priority = new IssueFilterGroup("priority", "Priority", "multiselect");
        priority.setOptions(filter.getPossiblePriorities().stream().map(it -> new IssueFilterOptions(it.name(), it.name())).collect(Collectors.toList()));
        filter.filters.add(priority);
        //Date
        filter.filters.add(new IssueFilterGroup("created", "Created", "date", "ANY"));
        filter.filters.add(new IssueFilterGroup("updated", "Updated", "date", "ANY"));
        filter.filters.add(new IssueFilterGroup("due_date", "Due", "date", "ANY"));
        filter.filters.add(new IssueFilterGroup("start_date", "Start Date", "date", "ANY"));
        filter.filters.add(new IssueFilterGroup("end_date", "End Date", "date", "ANY"));
        return filter;
    }

    public List<IssueFilter> getFilters(String projectKey) {
        return issueFilterRepo.findByOwnerOrOpenTrue(currentLogin.getUser()).stream()
                .filter(f -> f.getProject().getId().equals(projectService.findByKey(projectKey).getId()))
                .peek(f -> f.setReadonly(!f.getOwner().equals(currentLogin.getUser())))
                .sorted(Comparator.comparing(IssueFilter::getName)).collect(Collectors.toList());
    }

    public List<IssueFilter> getOpenFilters(String projectKey) {
        return issueFilterRepo.findByOpenTrue().stream()
                .filter(f -> f.getProject().getId().equals(projectService.findByKey(projectKey).getId()))
                .sorted(Comparator.comparing(IssueFilter::getName)).collect(Collectors.toList());
    }

    public Optional<IssueFilter> getFilter(Long id) {
        Optional<IssueFilter> f = issueFilterRepo.findById(id);
        f.ifPresent(filter -> filter.setReadonly(!filter.getOwner().equals(currentLogin.getUser())));
        return f;
    }

    public IssueFilterDTO getPossibleOptions(IssueFilterDTO filter) {
        //Issue Types
        if (filter.getProjects().isEmpty())
            filter.setPossibleIssueTypes(issueTypeRepo.findAllByProject(filter.getPossibleProjects().stream().map(Project::getId).collect(Collectors.toSet())));
        else
            filter.setPossibleIssueTypes(issueTypeRepo.findAllByProject(filter.getProjects().stream().map(Project::getId).collect(Collectors.toSet())));

        //Resolution
        Set<Resolution> resolutions = new HashSet<>(resolutionService.getAll());
        resolutions.add(new Resolution(0L, "unresolved"));
        filter.setPossibleResolutions(resolutions);

        //Label
        filter.setPossibleLabels(labelRepo.findAll());

        //Components
        filter.setPossibleComponents(componentService.findAllForProjects(filter.getProjects().stream().map(Project::getId).collect(Collectors.toSet())));

        //Version
        List<Version> version = new ArrayList<>();
        if (filter.getProjects().isEmpty())
            filter.getPossibleProjects().forEach(p -> version.addAll(versionService.getAllVersionForProject(p)));
        else
            filter.getProjects().forEach(p -> version.addAll(versionService.getAllVersionForProject(p)));
        filter.setPossibleVersions(version);

        //Members
        Set<Login> members = filter.getPossibleMembers();
        members.add(new Login(0L, "unassigned", "", "unassigned"));
        filter.setPossibleStatus(issueTypeService.getAllStatus());
        if (filter.getProjects().isEmpty())
            filter.getPossibleProjects().forEach(p -> members.addAll(authService.getProjectMembers(p)));
        else
            filter.getProjects().forEach(p -> members.addAll(authService.getProjectMembers(p)));
        filter.setAssignee(members.stream().filter(m -> currentLogin.getUser().equals(m)).collect(Collectors.toSet()));
        return filter;
    }

    public IssueSearchDTO assignedToMe() {
        IssueFilter issueFilter = new IssueFilter("Assigned to me", currentLogin.getUser(), "Updated", "DESC", null, true, null);
        ArrayList<IssueSearchQueryRule> rules = new ArrayList<>();
        rules.add(new IssueSearchQueryRule("resolution", "IN", new String[]{"0"})); // unresolved
        rules.add(new IssueSearchQueryRule("assignee", "IN", new String[]{currentLogin.getUser().getId().toString()}));
        issueFilter.setQuery(new IssueSearchQuery("and", rules));
        IssueSearchDTO issueSearchDTO = new IssueSearchDTO(null, "Updated", "DESC", currentLogin.getUser().getTimezone().toString(), issueFilter, 1, 10);
        return search(null, issueSearchDTO, "", projectService.getViewableProjects());
    }

    public IssueSearchDTO due(Integer days) {
        IssueFilter issueFilter = new IssueFilter("Due", currentLogin.getUser(), "Due", "ASC", null, true, null);
        ArrayList<IssueSearchQueryRule> rules = new ArrayList<>();
        rules.add(new IssueSearchQueryRule("resolution", "IN", new String[]{"0"})); // unresolved
        //rules.add(new IssueSearchQueryRule("due_date", "ALREADY", null));
        if (days > 0) {
            IssueSearchQueryRule d = new IssueSearchQueryRule("due_date", "CUSTOM", null);
            //d.setValueFrom(DateTime.now().plusDays(days).toString());
            d.setValueTo(DateTime.now().plusDays(days).toString());
            rules.add(d);
        } else // get already due only
            rules.add(new IssueSearchQueryRule("due_date", "ALREADY", null));
        issueFilter.setQuery(new IssueSearchQuery("and", rules));
        IssueSearchDTO issueSearchDTO = new IssueSearchDTO(null, "Due", "ASC", currentLogin.getUser().getTimezone().toString(), issueFilter, 1, 10);
        return search(null, issueSearchDTO, "", projectService.getViewableProjects());
    }

    public Set<Issue> searchIssues(String projectKey, String q) {
        Project project = projectRepo.findByKey(projectKey);
        if (null == project) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (ObjectUtils.isEmpty(q)) {
            return issueRepo.getLatestByProject(project.getId(), 100);
        }
        return issueRepo.containsTextByProject(q.toLowerCase(), project.getId());
    }

    public IssueSearchDTO searchDueIssues(String projectKey, String from, String to) {
        Project project = projectService.findByKey(projectKey);
        IssueFilter issueFilter = new IssueFilter("Get Due", currentLogin.getUser(), "Updated", "DESC", null, true, project);
        ArrayList<IssueSearchQueryRule> rules = new ArrayList<>();
        issueFilter.setQuery(new IssueSearchQuery("and", rules));
        IssueSearchDTO issueSearchDTO = new IssueSearchDTO();
        issueSearchDTO.setPageIndex(1);
        issueSearchDTO.setPageSize(100000000);
        issueSearchDTO.setFilter(issueFilter);
        issueSearchDTO.setProjectKey(projectKey);
        issueSearchDTO.setTimezone(currentLogin.getUser().getTimezone().getID());
        if (!canView(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueSearchDTO.setProjectKey(projectKey);
        try {
            ZoneId zone = ZoneId.of(currentLogin.getUser().getTimezone().getID());
            ZonedDateTime reportFrom = LocalDateTime.of(LocalDate.parse(from), LocalTime.of(0, 0, 0)).atZone(zone);
            ZonedDateTime reportTo = LocalDateTime.of(LocalDate.parse(to), LocalTime.of(23, 59, 59)).atZone(zone);
            ZoneId zoneUTC = ZoneId.of("UTC");
            reportFrom = reportFrom.withZoneSameInstant(zoneUTC);
            reportTo = reportTo.withZoneSameInstant(zoneUTC);
            from = reportFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            to = reportTo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String range = "(due_date between '" + from + "' AND '" + to + "')";
            return searchIssuesLucene(project.getKey(), issueSearchDTO, range);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JDException("Invalid query", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    public IssueSearchDTO searchIssuesBetween(String projectKey, String from, String to) {
        Project project = projectService.findByKey(projectKey);
        IssueFilter issueFilter = new IssueFilter("Get Issues", currentLogin.getUser(), "Updated", "DESC", null, true, project);
        ArrayList<IssueSearchQueryRule> rules = new ArrayList<>();
        issueFilter.setQuery(new IssueSearchQuery("and", rules));
        IssueSearchDTO issueSearchDTO = new IssueSearchDTO();
        issueSearchDTO.setPageIndex(1);
        issueSearchDTO.setPageSize(100000000);
        issueSearchDTO.setFilter(issueFilter);
        issueSearchDTO.setProjectKey(projectKey);
        issueSearchDTO.setTimezone(currentLogin.getUser().getTimezone().getID());
        if (!canView(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueSearchDTO.setProjectKey(projectKey);
        try {
            ZoneId zone = ZoneId.of(currentLogin.getUser().getTimezone().getID());
            ZonedDateTime reportFrom = LocalDateTime.of(LocalDate.parse(from), LocalTime.of(0, 0, 0)).atZone(zone);
            ZonedDateTime reportTo = LocalDateTime.of(LocalDate.parse(to), LocalTime.of(23, 59, 59)).atZone(zone);
            ZoneId zoneUTC = ZoneId.of("UTC");
            reportFrom = reportFrom.withZoneSameInstant(zoneUTC);
            reportTo = reportTo.withZoneSameInstant(zoneUTC);
            from = reportFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            to = reportTo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String range = "((start_date between '" + from + "' AND '" + to + "')" +
                    " OR (end_date between '" + from + "' AND '" + to + "'))";
            return searchIssuesLucene(project.getKey(), issueSearchDTO, range);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JDException("Invalid query", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    public IssueSearchDTO searchIssuesLucene(String projectKey, IssueSearchDTO issueSearchDTO, String range) {
        Project project = projectRepo.findByKey(projectKey);
        if (null == project) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueSearchDTO.setProjectKey(projectKey);
        try {
            return search(project, issueSearchDTO, range, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JDException("Invalid query", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    private IssueSearchDTO search(Project project, IssueSearchDTO issueSearchDTO, String range, List<Project> projects) {
        StringBuilder q = new StringBuilder();
        TimeZone tz = TimeZone.getTimeZone(issueSearchDTO.getTimezone());
        if (issueSearchDTO.getFilter().getQuery().getRules() != null) {
            issueSearchDTO.getFilter().getQuery().getRules().forEach(rule -> {
                if (q.length() > 0)
                    q.append(" ").append(issueSearchDTO.getFilter().getQuery().getCondition().toUpperCase()).append(" ");
                if (rule.getField().equalsIgnoreCase("created") ||
                        rule.getField().equalsIgnoreCase("updated") ||
                        rule.getField().equalsIgnoreCase("due_date") ||
                        rule.getField().equalsIgnoreCase("start_date") ||
                        rule.getField().equalsIgnoreCase("end_date")) {
                    switch (rule.getOperator()) {
                        case "TODAY":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "THIS WEEK":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).dayOfWeek().withMinimumValue().toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "LAST WEEK":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).minusWeeks(1).dayOfWeek().withMinimumValue().toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).minusWeeks(1).dayOfWeek().withMaximumValue().toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "THIS MONTH":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).dayOfMonth().withMinimumValue().toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).dayOfMonth().withMaximumValue().toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "LAST MONTH":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).minusMonths(1).dayOfMonth().withMinimumValue().toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).minusMonths(1).dayOfMonth().withMaximumValue().toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "THIS YEAR":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).dayOfYear().withMinimumValue().toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).dayOfYear().withMaximumValue().toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "LAST YEAR":
                            q.append(rule.getField()).append(" between '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(0, 0, 0, 0).minusYears(1).dayOfYear().withMinimumValue().toDateTime(DateTimeZone.UTC).toString());
                            q.append("' AND '");
                            q.append(DateTime.now(DateTimeZone.forTimeZone(tz)).withTime(23, 59, 59, 59).minusYears(1).dayOfYear().withMaximumValue().toDateTime(DateTimeZone.UTC).toString()).append("'");
                            break;
                        case "ALREADY":
                            q.append(" ").append(" due_date < NOW() ");
                            break;
                        case "CUSTOM":
                            StringBuilder cq = new StringBuilder();
                            if (!ObjectUtils.isEmpty(rule.getValueFrom())) {
                                cq.append(rule.getField()).append(" > '");
                                cq.append(DateTime.parse(rule.getValueFrom()).toDateTime(DateTimeZone.UTC).toString()).append("'");
                            }
                            if (!ObjectUtils.isEmpty(rule.getValueTo())) {
                                if (cq.length() > 0)
                                    cq.append(" AND ");
                                cq.append(rule.getField()).append(" < '");
                                cq.append(DateTime.parse(rule.getValueTo()).toDateTime(DateTimeZone.UTC).toString()).append("'");
                            }
                            q.append("(").append(cq.toString()).append(")");
                            break;
                    }
                } else {
                    switch (rule.getOperator()) {
                        case "IN":
                            if (rule.getField().equalsIgnoreCase("current_step")) {
                                List<String> values = new ArrayList<>();
                                workflowService.getAllWorkflowSteps().stream().filter(ws -> Arrays.asList(rule.getValues()).contains(ws.getIssueStatus().getId().toString())).forEach(workflowStep -> values.add(workflowStep.getId().toString()));
                                q.append(rule.getField()).append(" IN (").append(String.join(",", values)).append(")");
                            } else if (rule.getField().equalsIgnoreCase("resolution") || rule.getField().equalsIgnoreCase("assignee")) {
                                StringBuilder sq = new StringBuilder();
                                for (String value : rule.getValues()) {
                                    if (sq.length() > 0) sq.append(" OR ");
                                    if (value.equalsIgnoreCase("unresolved") || value.equalsIgnoreCase("unassigned") || value.equalsIgnoreCase("0"))
                                        sq.append(rule.getField()).append(" is null ");
                                    else
                                        sq.append(rule.getField()).append(" IN (").append(value).append(")");
                                }
                                q.append(" (").append(sq.toString()).append(") ");
                            } else if (rule.getField().equalsIgnoreCase("priority")) {
                                StringBuilder sq = new StringBuilder();
                                for (String value : rule.getValues()) {
                                    if (sq.length() > 0) sq.append(" OR ");
                                    sq.append(rule.getField()).append(" = '").append(value).append("' ");
                                }
                                q.append(" (").append(sq.toString()).append(") ");
                            } else if (rule.getField().equalsIgnoreCase("label") || rule.getField().equalsIgnoreCase("version") || rule.getField().equalsIgnoreCase("component")) {
                                StringBuilder sq = new StringBuilder();
                                for (String value : rule.getValues()) {
                                    if (sq.length() > 0) sq.append(" OR ");
                                    sq.append("data->'").append(rule.getField()).append("' @> '").append(value).append("' ");
                                }
                                q.append(" (").append(sq.toString()).append(") ");
                            } else
                                q.append(rule.getField()).append(" IN (").append(String.join(",", rule.getValues())).append(")");
                            break;
                        case "NOT IN":
                            if (rule.getField().equalsIgnoreCase("current_step")) {
                                List<String> values = new ArrayList<>();
                                workflowService.getAllWorkflowSteps().stream().filter(ws -> Arrays.asList(rule.getValues()).contains(ws.getIssueStatus().getId().toString())).forEach(workflowStep -> values.add(workflowStep.getId().toString()));
                                q.append(rule.getField()).append(" NOT IN (").append(String.join(",", values)).append(")");
                            } else if (rule.getField().equalsIgnoreCase("resolution") || rule.getField().equalsIgnoreCase("assignee")) {
                                StringBuilder sq = new StringBuilder();
                                for (String value : rule.getValues()) {
                                    if (sq.length() > 0) sq.append(" AND ");
                                    if (value.equalsIgnoreCase("unresolved") || value.equalsIgnoreCase("unassigned") || value.equalsIgnoreCase("0"))
                                        sq.append(rule.getField()).append(" is not null ");
                                    else
                                        sq.append(rule.getField()).append(" NOT IN (").append(value).append(")");
                                }
                                q.append(" (").append(sq.toString()).append(") ");
                            } else if (rule.getField().equalsIgnoreCase("priority")) {
                                StringBuilder sq = new StringBuilder();
                                for (String value : rule.getValues()) {
                                    if (sq.length() > 0) sq.append(" AND ");
                                    sq.append(rule.getField()).append(" != '").append(value).append("'");
                                }
                                q.append(" (").append(sq.toString()).append(") ");
                            } else if (rule.getField().equalsIgnoreCase("label") || rule.getField().equalsIgnoreCase("version") || rule.getField().equalsIgnoreCase("component")) {
                                StringBuilder sq = new StringBuilder();
                                for (String value : rule.getValues()) {
                                    if (sq.length() > 0) sq.append(" OR ");
                                    sq.append("data->'").append(rule.getField()).append("' @> '").append(value).append("'");
                                }
                                q.append(" NOT(").append(sq.toString()).append(") ");
                            } else
                                q.append(rule.getField()).append(" NOT IN (").append(String.join(",", rule.getValues())).append(")");
                            break;
                    }
                }
            });
        }
        String textFilter = !ObjectUtils.isEmpty(issueSearchDTO.getFilter().getSearchQuery())
                ? " AND content_vector @@ to_tsquery('"
                + issueSearchDTO.getFilter().getSearchQuery().replaceAll(" ", "|") + "')" : "";
        String sortBy = issueSearchDTO.getFilter().getSortBy().equalsIgnoreCase("due") ? "due_date" : issueSearchDTO.getFilter().getSortBy().toLowerCase();
        String finalQuery = null;
        if (null != projects && !projects.isEmpty()) {
            finalQuery = "project IN (" + projects.stream().map(p -> p.getId().toString()).collect(Collectors.joining(",")) + ")";
        } else {
            finalQuery = "project = " + project.getId();
        }
        finalQuery = finalQuery + (q.length() > 0 ? " AND (" + q.toString() + ")" : "") + (range.isEmpty() ? "" : " AND " + range) + textFilter;
        String backingQuery = " order by " + sortBy + " " + issueSearchDTO.getFilter().getSortOrder()
                + " limit " + issueSearchDTO.getPageSize() + " offset " + ((issueSearchDTO.getPageIndex() - 1) * issueSearchDTO.getPageSize());
        issueSearchDTO.setJql(finalQuery);
        issueSearchDTO.setIssues(issueSearchCustomRepo.filter(finalQuery, backingQuery));
        issueSearchDTO.setTotal(issueSearchCustomRepo.count(finalQuery).longValue());
        issueSearchDTO.setIssueKeys(issueSearchCustomRepo.filterOnlyKeys(finalQuery, ""));
        /*//Expand values
        issueSearchDTO.getFilter().getQuery().getRules().forEach(r -> {
            List<String> eValues = new ArrayList<>();
            switch (r.getField()) {
                case "resolution" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    if (vv > 0)
                        resolutionService.findById(vv).ifPresent(ev -> eValues.add(ev.getName()));
                    else if (vv == 0)
                        eValues.add("Unresolved");
                });
                case "assignee", "reporter" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    if (vv > 0)
                        Optional.ofNullable(userService.find(vv)).ifPresent(ev -> eValues.add(ev.getFullName()));
                    else if (vv == 0)
                        eValues.add("unassigned");
                });
                case "component" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    Optional.ofNullable(componentService.get(issueSearchDTO.getProjectKey(), vv)).ifPresent(ev -> eValues.add(ev.getName()));
                });
                case "label" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    labelService.get(vv).ifPresent(ev -> eValues.add(ev.getName()));
                });
                case "version" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    versionService.get(vv).ifPresent(ev -> eValues.add(ev.getName()));
                });
                case "issue_type" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    issueTypeService.findOne(vv).ifPresent(ev -> eValues.add(ev.getName()));
                });
                case "current_step" -> Arrays.stream(r.getValues()).forEach(v -> {
                    long vv = Long.parseLong(v);
                    issueTypeService.findStatus(vv).ifPresent(ev -> eValues.add(ev.getName()));
                });
                case "priority" -> eValues.addAll(Arrays.asList(r.getValues()));
            }
            r.setExpandedValues(eValues);
        });*/
        return issueSearchDTO;
    }

    public IssueSearchDTO searchIssuesLucene(String projectKey, IssueSearchDTO issueSearchDTO) {
        return searchIssuesLucene(projectKey, issueSearchDTO, "");
    }

    public IssueFilter saveFilter(String projectKey, IssueFilter filter) {
        Project project = projectService.findByKey(projectKey);
        if (filter.getId() != null) {
            IssueFilter orgFilter = issueFilterRepo.getOne(filter.getId());
            if (!orgFilter.getOwner().equals(currentLogin.getUser()))
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            if (filter.isOpen() != orgFilter.isOpen() && !boardService.getByFilter(orgFilter).isEmpty())
                throw new JDException("Filter cannot be made " + (filter.isOpen() ? " Public" : "Project"), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
            orgFilter.setQuery(filter.getQuery());
            orgFilter.setName(filter.getName());
            orgFilter.setOpen(filter.isOpen());
            orgFilter.setSortBy(filter.getSortBy());
            orgFilter.setSortOrder(filter.getSortOrder());
            orgFilter.setSearchQuery(filter.getSearchQuery());
            return issueFilterRepo.save(orgFilter);
        } else {
            filter.setProject(project);
            filter.setOwner(currentLogin.getUser());
            return issueFilterRepo.save(filter);
        }
    }

    @Cacheable(value = "issueMinimal", key = "#projectKey + #issueKey")
    public Issue getIssueMinimal(String projectKey, Long issueKey) {
        Project project = projectService.findByKey(projectKey);
        Issue i = new Issue(issueRepo.findMinimalByProjectAndKey(project, issueKey), project);
        if (i.getId() == null)
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        return i;
    }

    @Cacheable(value = "issueMinimal", key = "#projectKey + #issueKey")
    public Issue getMinimal(String projectKey, Long issueKey) {
        Project project = projectService.findByKey(projectKey);
        return issueRepo.findByProjectAndKey(project, issueKey);
    }

    public Issue get(String projectKey, Long issueKey) {
        Project project = projectService.getProjectByKey(projectKey);
        Issue issue = issueRepo.findByProjectAndKey(project, issueKey);
        if (null == issue) throw new JDException("Issue not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        issue.setPermissions(new HashMap<>());
        issue.setWatchers(watchersRepo.findByIssue(issue));
        issue.setProject(project);
        if (canEdit(issue.getProject())) issue.getPermissions().put("edit", true);
        if (canDelete(issue.getProject())) issue.getPermissions().put("delete", true);
        if (canComment(issue.getProject())) issue.getPermissions().put("comment", true);
        if (canEdit(issue.getProject()) && canChangeAssignee(issue.getProject()))
            issue.getPermissions().put("assign", true);
        if (canEdit(issue.getProject()) && canChangeReporter(issue.getProject()))
            issue.getPermissions().put("reporter", true);
        if (canLink(issue.getProject())) issue.getPermissions().put("link", true);
        if (hasAccess(issue.getProject(), AuthorityCode.ATTACHMENT_CREATE)) issue.getPermissions().put("attach", true);
        if (canTransitionIssue(issue)) issue.getPermissions().put("transition", true);
        if (hasAccess(issue.getProject(), AuthorityCode.ATTACHMENT_DELETE_ALL))
            issue.getPermissions().put("attach_d_a", true);
        if (hasAccess(issue.getProject(), AuthorityCode.ATTACHMENT_DELETE_OWN))
            issue.getPermissions().put("attach_d_o", true);
        if (canTransitionIssue(issue))
            issue.setPossibleTransitions(workflowService.getPossibleTransitions(issue.getIssueType().getWorkflow(), issue.getCurrentStep()));
        TimeTracking tts = projectService.getTimeTrackingSettings(issue.getProject());
        issue.setEstimateString(workLogService.minutesToString(issue.getTimeOriginalEstimate(), tts));
        issue.setTimeSpent(workLogService.getWorkLoggedForIssue(issue));
        issue.setTimeSpentString(workLogService.minutesToString(issue.getTimeSpent(), tts));
        issue.setPossibleVersions(versionService.getAllVersionForProject(project));
        issue.setPossibleComponents(componentService.getAllForProject(projectKey));
        issue.setBranchCount(gitBranchRepo.countByIssuesContainingIgnoreCase(issue.getKeyPair()));
        issue.setCommitCount(gitCommitRepo.countByIssuesContainingIgnoreCase(issue.getKeyPair()));
        //Get CustomFields
        issue.setCustomFields(getCustomFields(project, issue));
        return issue;
    }

    /**
     * Register issue has been viewed
     *
     * @param issue
     */
    public void registerIssueView(Issue issue) {
        issueEventService.sendMessage(new IssueEvent(IssueEventType.VIEW, issue, null, null, null, currentLogin.getUser()));
    }

    public byte[] export(String projectKey, IssueSearchDTO issueSearchDTO, HttpServletRequest request, HttpServletResponse response, String type) throws Exception {
        Project project = projectRepo.findByKey(projectKey);
        if (null == project) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueSearchDTO.setPageIndex(1);
        issueSearchDTO.setPageSize(1000000000);
        issueSearchDTO.setProjectKey(projectKey);
        IssueSearchDTO issues = search(project, issueSearchDTO, "", null);
        Workbook workbook = new HSSFWorkbook();
        if (type.equals("csv")) {
            ByteArrayInputStream in = new ExcelExportView().buildCSVDocument(issues.getIssues(), workbook, request, response);
            return IOUtils.toByteArray(in);
        } else {
            ByteArrayInputStream in = new ExcelExportView().buildExcelDocument(issues.getIssues(), workbook, request, response);
            return IOUtils.toByteArray(in);
        }
    }

    public Set<IssueHistory> getHistory(String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (null == issue) throw new JDException("Issue not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueHistoryRepo.findByIssueOrderByUpdatedDesc(issue);
    }

    public Set<Issue> findByProject(Long project_id) {
        Project project = projectService.getProject(project_id);
        if (null == project) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueRepo.findByProject(project);
    }

    public Set<Issue> findByDueDateBeforeAndEqual(LocalDate date) {
        return issueRepo.findAllByDueDateLessThanEqual(date);
    }

    public IssueUpdate create(Issue issue, Long projectID) {
        Project project = projectService.getProject(projectID);
        if (!canCreate(project)) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issue.setProject(project);
        Optional<IssueType> issueType = issueTypeRepo.findById(issue.getIssueType().getId());
        if (issueType.isEmpty()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!issueType.get().isActive())
            throw new JDException("Issue type is not active", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        Issue i = new Issue(issueType.get(), issue.getSummary(), issue.getDescription(), project, issue.getReporter() == null ? authService.currentLogin() : issue.getReporter(), issue.getPriority());
        i.setAssignee(issue.getAssignee());
        if (issue.getDueDate() != null)
            i.setDueDate(issue.getDueDate());
        Set<ConstraintViolation<Project>> result = validator.validate(issue.getProject());
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //Get workflow initial step and assign as current step
        i.setCurrentStep(issueType.get().getWorkflow().getDefaultStep());
        if (i.getDescription() == null) i.setDescription("");
        //i.setDescription(replaceInsertsPOST(issue, "", i.getDescription(), "description"));
        //Create custom fields
        //i.setCustomFields(getCustomFields(project, i));
        //Loop save until created
        boolean skip = false;
        int count = 0;
        Date lastUpdatedDate = i.getUpdated();
        do {
            try {
                i.setKey(getLastKeyForProject(project) + 1);
                i.setKeyPair(project.getKey() + "-" + i.getKey());

                i = save(i);
            } catch (ConstraintViolationException | DataIntegrityViolationException e) {
                log.info("Violation saving issue with keyPair: " + i.getKeyPair() + " : " + e.getMessage());
            } catch (Exception e) {
                skip = true;
                log.info("Error saving issue with keyPair: " + i.getKeyPair() + " : " + e.getMessage());
                e.printStackTrace();
            } finally {
                count++;
            }
        } while (i.getId() == null && !skip && count < 20);
        //Set custom Fields
        getCustomFields(project, i);
        //Add reporter as watcher
        watchersRepo.save(new Watchers(i, issue.getReporter()));
        //Add assignee as watcher if exists
        issueAsyncService.addWatcher(i, issue.getAssignee());
        //Log create event
        logHistory(IssueEventType.CREATE, i, "issue", "created", null, "");
        IssueUpdate iu = new IssueUpdate(true, lastUpdatedDate, i.getUpdated());
        iu.setIssue(i);
        return iu;
    }

    public Set<IssueCustomField> getCustomFields(Project project, Issue issue) {
        JSONObject customData = new JSONObject();
        try {
            if (!ObjectUtils.isEmpty(issue.getCustomData()))
                customData = new JSONObject(issue.getCustomData());
        } catch (Exception ignored) {
        }
        Set<IssueCustomField> customFields = new LinkedHashSet<>();
        JSONObject finalCustomData = customData;
        customFieldRepo.findByProjectAndIssueTypesOrderByNameAsc(project, issue.getIssueType()).forEach(c -> {
            IssueCustomField icf = new IssueCustomField(null, c, null, null);
            if (finalCustomData.has(c.getKey()))
                icf.setValue(finalCustomData.getString(c.getKey()));
            else if (!ObjectUtils.isEmpty(c.getDefaultValue())) {
                finalCustomData.put(c.getKey(), c.getDefaultValue());
                issueCustomRepo.updateCustomDataByIssue(issue.getId(), "array['" + c.getKey() + "']",
                        '\"' + c.getDefaultValue() + '\"');
                icf.setValue(finalCustomData.getString(c.getKey()));
            }
            customFields.add(icf);
        });
        return customFields;
    }

    @CacheEvict(value = "issueMinimal", allEntries = true)
    public IssueUpdate updateCustomField(IssueCustomField customField, Long issueKey, String projectKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        Date lastUpdatedDate = issue.getUpdated();
        if (!canEdit(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        JSONObject customData = new JSONObject();
        try {
            if (!ObjectUtils.isEmpty(issue.getCustomData()))
                customData = new JSONObject(issue.getCustomData());
            else {
                issue.setCustomData(customData.toString());
                issueRepo.save(issue);
            }
        } catch (Exception ignored) {
        }
        String oldVal = customData.has(customField.getCustomField().getKey()) ? customData.getString(customField.getCustomField().getKey()) : "";
        //Required validation
        if (customField.getCustomField().isRequired()) {
            if ((customField.getCustomField().isMultiple() && customField.getValues().length <= 0) || (!customField.getCustomField().isMultiple() && customField.getValue().isEmpty()))
                throw new JDException(customField.getCustomField().getName() + " field is required", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //Pattern validation
        if (customField.getCustomField().getValidation() != null && !customField.getCustomField().getValidation().isEmpty()) {
            final Pattern pattern = Pattern.compile(customField.getCustomField().getValidation());
            if (!pattern.matcher(customField.getValue()).matches()) {
                throw new JDException(customField.getCustomField().getName() + " field is invalid", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
            }
        }
        issueCustomRepo.updateCustomDataByIssue(issue.getId(), "array['" + customField.getCustomField().getKey() + "']",
                '"' + (customField.getCustomField().isMultiple() ? String.join(",", customField.getValues()) : customField.getValue()) + '"');
        String f = customField.getCustomField().getName() + "(" + customField.getCustomField().getKey() + ")";
        logHistory(IssueEventType.UPDATE, issue, f, "updated field " + f, oldVal, customField.getValue());
        save(issue);
        issue = getMinimal(projectKey, issueKey);
        return new IssueUpdate(true, lastUpdatedDate, issue.getUpdated(), issue);
    }

    @CacheEvict(value = "issueMinimal", allEntries = true)
    public List<String> quickUpdate(QuickUpdate data) {
        List<String> errors = new ArrayList<>();
        AtomicBoolean changed = new AtomicBoolean(false);
        data.getIssuesKeyPairs().forEach(i -> {
            Issue issue = getMinimal(getProjectKeyFromPair(i), getIssueKeyFromPair(i));
            if (!canEdit(issue.getProject())) {
                errors.add("Cannot edit " + issue.getKeyPair());
                return;
            }
            switch (data.getField()) {
                case "priority":
                    try {
                        String oldVal = issue.getPriority().name();
                        issue.setPriority(Priority.valueOf(data.getData()));
                        logHistory(IssueEventType.UPDATE, issue, "priority", "updated priority", oldVal, issue.getPriority().name());
                        changed.set(true);
                    } catch (Exception e) {
                        errors.add("Invalid priority " + data.getData() + " for " + issue.getKeyPair());
                    }
                    break;
                case "datepair":
                    try {
                        if (data.getData().contains(":") && data.getData().length() == 21) {
                            LocalDate oldStartVal = issue.getStartDate();
                            LocalDate oldEndVal = issue.getEndDate();
                            LocalDate startDate = LocalDate.parse(data.getData().split(":")[0]);
                            LocalDate endDate = LocalDate.parse(data.getData().split(":")[1]);
                            issue.setStartDate(startDate);
                            issue.setEndDate(endDate);
                            if (endDate.isBefore(startDate))
                                throw new JDException("End date should be after start", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                            logHistory(IssueEventType.UPDATE, issue, "start date", "updated start date", oldStartVal + "", startDate + "");
                            logHistory(IssueEventType.UPDATE, issue, "end date", "updated end date", oldEndVal + "", endDate + "");
                            changed.set(true);
                        } else {
                            errors.add("Invalid date pair " + data.getData() + " for " + issue.getKeyPair());
                        }
                    } catch (Exception e) {
                        errors.add("Invalid date pair " + data.getData() + " for " + issue.getKeyPair());
                    }
                    break;
                case "version":
                    try {
                        Set<Version> oldVal = issue.getVersions();
                        Optional.ofNullable(versionService.getVersionForProjectNoCache(issue.getProject(), Long.parseLong(data.getData())))
                                .ifPresent(v -> {
                                    Set<Version> newV = new HashSet<>(oldVal);
                                    newV.add(v);
                                    issue.setVersions(newV);
                                    logHistory(IssueEventType.UPDATE, issue, "version", "changed version",
                                            StringUtils.collectionToCommaDelimitedString(oldVal.stream().map(Version::getName).collect(Collectors.toList())),
                                            StringUtils.collectionToCommaDelimitedString(issue.getVersions().stream().map(Version::getName).collect(Collectors.toList())));
                                    changed.set(true);
                                });
                        /*Set<Version> versions = versionService.getAllVersionForProjectNoCache(issue.getProject());
                        versions.stream().filter(v -> v.getId() == Long.parseLong(data.getData())).findAny().ifPresent(v -> {
                            List<Version> newV = versions.stream().filter(v2 -> oldVal.stream().map(Version::getId)
                                    .collect(Collectors.toList()).contains(v2.getId())).collect(Collectors.toList());
                            newV.add(v);
                            issue.setVersions(newV);
                            logHistory(IssueEventType.UPDATE, issue, "version", "changed version",
                                    StringUtils.collectionToCommaDelimitedString(oldVal.stream().map(Version::getName).collect(Collectors.toList())),
                                    StringUtils.collectionToCommaDelimitedString(issue.getVersions().stream().map(Version::getName).collect(Collectors.toList())));
                            changed.set(true);
                        });*/
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors.add("Invalid version " + data.getData() + " for " + issue.getKeyPair());
                    }
                    break;
                case "assign":
                    if (!canChangeAssignee(issue.getProject())) {
                        errors.add("Cannot set assignee for " + issue.getKeyPair());
                        return;
                    }
                    Login currentAssignee;
                    switch (data.getData()) {
                        case "0":
                            String oldVal = null == issue.getAssignee() ? "" : issue.getAssignee().getUserName();
                            issue.setAssignee(null);
                            logHistory(IssueEventType.ASSIGN, issue, "assignee", "assigned issue", oldVal, "");
                            changed.set(true);
                            break;
                        case "-1":
                            Login currentuser = userService.getInternal(currentLogin.getUser().getId());
                            issueAsyncService.addWatcher(issue, currentuser);
                            currentAssignee = issue.getAssignee();
                            String oldAssigneeVal = null == issue.getAssignee() ? "" : issue.getAssignee().getUserName();
                            issue.setAssignee(currentuser);
                            if (currentAssignee != null && currentAssignee.equals(issue.getAssignee()))
                                break;
                            if (currentAssignee == null) {
                                //Assign
                                logHistory(IssueEventType.ASSIGN, issue, "assignee", "assigned issue", oldAssigneeVal, currentuser.getUserName());
                                changed.set(true);
                            } else {
                                //Reassign
                                logHistory(IssueEventType.REASSIGN, issue, "assignee", "changed assignee", oldAssigneeVal, currentuser.getUserName());
                                changed.set(true);
                            }
                            break;
                        default:
                            Login assignee = userService.getUser(Long.parseLong(data.getData()));
                            currentAssignee = issue.getAssignee();
                            String oldAssigneVal = null == issue.getAssignee() ? null : issue.getAssignee().getUserName();
                            if (currentAssignee != null && currentAssignee.getId().equals(Long.parseLong(data.getData())))
                                break;
                            if (assignee != null) {
                                if (authService.getAuthorities(assignee).stream().filter(a -> assignee.isActive()).anyMatch(a -> a.getAuthorityCode().equals(AuthorityCode.PROJECT_VIEW))) {
                                    issueAsyncService.addWatcher(issue, assignee);
                                    issue.setAssignee(assignee);
                                    logHistory(IssueEventType.UPDATE, issue, "assignee", "changed assignee", oldAssigneVal, assignee.getUserName());
                                    changed.set(true);
                                } else throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.PRECONDITION_FAILED);
                            }
                            break;
                    }
                    break;
            }
            if (changed.get())
                save(issue);
        });
        return errors;
    }

    @CacheEvict(value = "issueMinimal", key = "#projectKey + #issueKey")
    public IssueUpdate update(Issue is, Long issueKey, String projectKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (!canEdit(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);

        if (!ObjectUtils.isEmpty(is.getSummary())) {
            String oldVal = issue.getSummary().replace("\n", "").replace("\r", "");
            issue.setSummary(is.getSummary());
            logHistory(IssueEventType.UPDATE, issue, "summary", "updated summary", oldVal, is.getSummary());
        }
        if (!ObjectUtils.isEmpty(is.getDescription())) {
            String oldDescription = issue.getDescription();
            String description = is.getDescription();
            //String description = replaceInsertsPOST(issue, oldDescription, is.getDescription(), "description");
            logHistory(IssueEventType.UPDATE, issue, "description", "updated description", oldDescription, description);
            //description = replaceInsertsPOST(issue, cleanPreserveLineBreaks(oldDescription), description, "description");
            issue.setDescription(description);
        }
        Optional.ofNullable(is.getPriority()).ifPresent(i -> {
            String oldVal = issue.getPriority().name();
            issue.setPriority(i);
            logHistory(IssueEventType.UPDATE, issue, "priority", "updated priority", oldVal, is.getPriority().name());
        });
        Optional.ofNullable(is.getDueDate()).ifPresent(i -> {
            String oldVal = null != issue.getDueDate() ? issue.getDueDate() + "" : "";
            issue.setDueDate((i.equals(LocalDate.parse("1901-01-31"))) ? null : i);
            logHistory(IssueEventType.UPDATE, issue, "due date", "updated due date", oldVal, is.getDueDate() + "");
        });
        Optional.ofNullable(is.getStartDate()).ifPresent(i -> {
            String oldVal = null != issue.getStartDate() ? i.equals(LocalDate.parse("1901-01-31")) ? null : issue.getStartDate() + "" : "";
            issue.setStartDate((i.equals(LocalDate.parse("1901-01-31"))) ? null : i);
            if (Optional.ofNullable(issue.getStartDate()).isPresent()
                    && Optional.ofNullable(issue.getEndDate()).isPresent()
                    && issue.getEndDate().isBefore(issue.getStartDate()))
                throw new JDException("Start date should be before end", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
            logHistory(IssueEventType.UPDATE, issue, "start date", "updated start date", oldVal, is.getStartDate() + "");
        });
        Optional.ofNullable(is.getEndDate()).ifPresent(i -> {
            String oldVal = null != issue.getEndDate() ? issue.getEndDate() + "" : "";
            issue.setEndDate((i.equals(LocalDate.parse("1901-01-31"))) ? null : i);
            if (Optional.ofNullable(issue.getStartDate()).isPresent()
                    && Optional.ofNullable(issue.getEndDate()).isPresent()
                    && issue.getStartDate().isAfter(issue.getEndDate()))
                throw new JDException("End date should be after start", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
            logHistory(IssueEventType.UPDATE, issue, "end date", "updated end date", oldVal, is.getEndDate() + "");
        });
        Optional.ofNullable(is.getEstimateString()).ifPresent(i -> {
            long oldVal = issue.getTimeOriginalEstimate();
            TimeTracking tts = projectService.getTimeTrackingSettings(issue.getProject());
            issue.setTimeOriginalEstimate(workLogService.stringToMinutes(is.getEstimateString(), tts));
            if (issue.getTimeOriginalEstimate() != oldVal)
                logHistory(IssueEventType.UPDATE, issue, "estimate", "updated estimate", workLogService.minutesToString(oldVal, tts), workLogService.minutesToString(issue.getTimeOriginalEstimate(), tts));
        });
        if (null != is.getReporter()) {
            if (canChangeReporter(issue.getProject())) {
                Login reporter = userService.getInternal(is.getReporter().getId());
                if (reporter != null) {
                    if (authService.getAuthorities(reporter).stream().anyMatch(a -> a.getAuthorityCode().equals(AuthorityCode.PROJECT_VIEW))) {
                        issueAsyncService.addWatcher(issue, reporter);
                        String oldVal = issue.getReporter().getUserName();
                        issue.setReporter(reporter);
                        logHistory(IssueEventType.UPDATE, issue, "reporter", "updated", oldVal, reporter.getUserName());
                    } else throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.PRECONDITION_FAILED);
                }
            }
        }

        if (null != is.getAssignee()) {
            if (canChangeAssignee(issue.getProject())) {
                Login currentAssignee;
                switch (is.getAssignee().getId().toString()) {
                    case "0":
                        String oldVal = null == issue.getAssignee() ? "" : issue.getAssignee().getUserName();
                        issue.setAssignee(null);
                        logHistory(IssueEventType.ASSIGN, issue, "assignee", "assigned issue", oldVal, "");
                        break;
                    case "-1":
                        Login currentuser = userService.getInternal(currentLogin.getUser().getId());
                        issueAsyncService.addWatcher(issue, currentuser);
                        currentAssignee = issue.getAssignee();
                        String oldUserVal = null == issue.getAssignee() ? "" : issue.getAssignee().getUserName();
                        issue.setAssignee(currentuser);
                        if (currentAssignee != null && currentAssignee.equals(is.getAssignee()))
                            break;
                        if (currentAssignee == null) {
                            //Assign
                            logHistory(IssueEventType.ASSIGN, issue, "assignee", "assigned issue", oldUserVal, currentuser.getUserName());
                        } else {
                            //Reassign
                            logHistory(IssueEventType.REASSIGN, issue, "assignee", "changed assignee", oldUserVal, currentuser.getUserName());
                        }
                        break;
                    default:
                        Login assignee = userService.getUser(is.getAssignee().getId());
                        currentAssignee = issue.getAssignee();
                        String oldAVal = null == issue.getAssignee() ? null : issue.getAssignee().getUserName();
                        if (currentAssignee != null && currentAssignee.equals(is.getAssignee()))
                            break;
                        if (assignee != null) {
                            if (authService.getAuthorities(assignee).stream().filter(a -> assignee.isActive()).anyMatch(a -> a.getAuthorityCode().equals(AuthorityCode.PROJECT_VIEW))) {
                                issueAsyncService.addWatcher(issue, assignee);
                                issue.setAssignee(assignee);
                                logHistory(IssueEventType.UPDATE, issue, "assignee", "changed assignee", oldAVal, assignee.getUserName());
                            } else throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.PRECONDITION_FAILED);
                        }
                        break;
                }
            }
        }
        if (null != is.getUpdateField() && is.getUpdateField().equalsIgnoreCase("versions")) {
            Set<Version> versions = new HashSet<>();
            Map<Long, Version> availableVersions = new HashMap<>();
            versionService.getAllVersionForProject(projectKey).forEach(v -> availableVersions.put(v.getId(), v));
            is.getVersions().stream().filter(v -> availableVersions.containsKey(v.getId())).forEach(versions::add);
            logHistory(IssueEventType.UPDATE, issue, "version", "changed version",
                    StringUtils.collectionToCommaDelimitedString(issue.getVersions().stream().map(Version::getName).collect(Collectors.toList())),
                    StringUtils.collectionToCommaDelimitedString(versions.stream().map(Version::getName).collect(Collectors.toList())));
            versions.forEach(v -> v.setProject(issue.getProject()));
            issue.setVersions(versions);
        }
        if (null != is.getUpdateField() && is.getUpdateField().equalsIgnoreCase("component")) {
            List<Component> components = new ArrayList<>();
            Map<Long, Component> availableComponents = new HashMap<>();
            componentService.getAllForProject(projectKey).forEach(v -> availableComponents.put(v.getId(), v));
            is.getComponents().stream().filter(v -> availableComponents.containsKey(v.getId())).forEach(components::add);
            logHistory(IssueEventType.UPDATE, issue, "component", "changed component",
                    StringUtils.collectionToCommaDelimitedString(issue.getComponents().stream().map(Component::getName).collect(Collectors.toList())),
                    StringUtils.collectionToCommaDelimitedString(components.stream().map(Component::getName).collect(Collectors.toList())));
            components.forEach(v -> v.setProject(issue.getProject()));
            issue.setComponents(components);
        }
        if (null != is.getUpdateField() && is.getUpdateField().equalsIgnoreCase("label")) {
            logHistory(IssueEventType.UPDATE, issue, "label", "changed label",
                    StringUtils.collectionToCommaDelimitedString(issue.getLabels().stream().map(Label::getName).collect(Collectors.toList())),
                    StringUtils.collectionToCommaDelimitedString(is.getLabels().stream().map(Label::getName).collect(Collectors.toList())));
            issue.setLabels(is.getLabels());
        }
        Set<ConstraintViolation<Issue>> result = validator.validate(issue);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        Date lastUpdatedDate = issue.getUpdated();
        Issue i = save(issue);
        i.setTimeSpent(workLogService.getWorkLoggedForIssue(i));
        if (i.getTimeSpent() > 0)
            i.setTimeSpentString(workLogService.minutesToString(i.getTimeSpent(), projectService.getTimeTrackingSettings(issue.getProject())));
        return new IssueUpdate(true, lastUpdatedDate, issueRepo.findUpdatedByProjectAndKey(issue.getProject(), issue.getKey()), i);
    }

    @CacheEvict(value = "issueMinimal", allEntries = true)
    @Transactional
    public Issue save(Issue issue) {
        issue = issueRepo.save(issue);
        issueEventService.updateIssue(issue);
        return issue;
    }

    @CacheEvict(value = "issueMinimal", key = "#projectKey + #issueKey")
    @Transactional
    public void delete(Long issueKey, String projectKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (!canDelete(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        commentRepo.findByIssueOrderByCreatedDateDesc(issue).forEach(c -> commentRepo.delete(c));
        issueHistoryRepo.findByIssueOrderByUpdatedDesc(issue).forEach(c -> issueHistoryRepo.delete(c));
        workLogService.deleteAll(issue);
        issueViewRepo.deleteByIssue(issue);
        relationshipService.deleteByIssue(issue);
        taskRepo.deleteAllByIssue(issue);
        attachmentRepo.findByIssue(issue).forEach(c -> {
            deleteAttachment(c, projectKey, issueKey);
            attachmentRepo.delete(c);
        });
        watchersRepo.findByIssue(issue).forEach(c -> watchersRepo.delete(c));
        issueEventService.deleteByIssue(issue);
        issueRepo.delete(issue);
    }

    @CacheEvict(value = "issueMinimal", key = "#projectKey + #issueKey")
    @Transactional
    public void changeIssueType(IssueType it, Long issueKey, String projectKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (!canEdit(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (issue.getIssueType().getId().equals(it.getId()))
            throw new JDException("Cannot set to same type", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        Optional<IssueType> issueType = issueTypeService.findOne(it.getId());
        issueType.ifPresent(itt -> {
            logHistory(IssueEventType.UPDATE, issue, "issue_type", "changed issue type", issue.getIssueType().getName(), itt.getName());
            issue.setIssueType(itt);
            issue.setCurrentStep(issueType.get().getWorkflow().getDefaultStep());
            issue.setResolution(null);
            issue.setResolvedDate(null);
            issue.setCustomFields(new HashSet<>());
            save(issue);
        });
    }

    @CacheEvict(value = "issueMinimal", allEntries = true)
    @Transactional
    public void delete(Long issueID) {
        Optional<Issue> issue = issueRepo.findById(issueID);
        if (issue.isEmpty())
            throw new JDException("Issue not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canDelete(issue.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        delete(issue.get().getKey(), issue.get().getProject().getKey());
    }

    private Long getLastKeyForProject(Project project) {
        Long lastKey;
        String seq = getProjectSequence(project);
        try {
            lastKey = issueRepo.findLastKeyBySeq(seq);
        } catch (Exception e) {
            log.warning("Error getting sequence for " + seq);
            createProjectSeq(project);
            lastKey = issueRepo.findLastKeyBySeq(seq);
        }
        return (null == lastKey) ? 0 : lastKey;
    }

    private String getProjectSequence(Project project) {
        return "seq_key_" + project.getId();
    }

    public void createProjectSeq(Project project) {
        String seq = getProjectSequence(project);
        issueCustomRepo.createSeq(seq, issueRepo.findLastKeyForProject(project));
    }

    private void validationCustomFields(Issue issue) {
        //Validate customFields
        issue.setCustomFields(getCustomFields(issue.getProject(), issue));
        issue.getCustomFields().stream().filter(cf -> cf.getCustomField().isRequired()).forEach(customField -> {
            if (customField.getCustomField().isRequired()) {
                if (customField.getCustomField().isMultiple()) {
                    if (ObjectUtils.isEmpty(customField.getValue()) || customField.getValue().split(",").length <= 0)
                        throw new JDException(customField.getCustomField().getName() + " field is required", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                } else {
                    if (ObjectUtils.isEmpty(customField.getValue()))
                        throw new JDException(customField.getCustomField().getName() + " field is required", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                }
            }
        });
    }

    @Transactional
    public Issue transition(Long issueKey, String projectKey, Long transitionID, String payload) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (!canTransitionIssue(issue))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        //Trigger custom field validation
        validationCustomFields(issue);
        Date prevUpdate = issue.getUpdated();
        List<Issue> issues = new ArrayList<>(Collections.singletonList(issue));
        Set<WorkflowTransition> possibleTransitions = workflowService.getPossibleTransitions(issue.getIssueType().getWorkflow(), issue.getCurrentStep());
        Optional<WorkflowTransition> tr = possibleTransitions.stream().filter(t -> t.getId().equals(transitionID)).findAny();
        if (tr.isEmpty()) {
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        } else {
            JSONArray fieldToUpdate = new JSONArray(payload);
            Set<WorkflowTransitionProperties> wp = workflowService.getWorkflowTransitionProperties(tr.get());
            Issue finalIssue = issue;
            wp.stream().filter(wpp -> wpp.getSubType().toString().startsWith("CONDITION")).forEach(trp -> {
                switch (trp.getSubType()) {
                    case CONDITION_CURRENT_USER:
                        Set<Long> matchableUsers = new HashSet<>();
                        Arrays.stream(trp.getValue().split(",")).forEach(v -> {
                            if (v.equals("-assignee-") && finalIssue.getAssignee() != null) {
                                matchableUsers.add(finalIssue.getAssignee().getId());
                            } else if (v.equals("-reporter-")) {
                                matchableUsers.add(finalIssue.getReporter().getId());
                            } else if (v.equals("-lead-")) {
                                matchableUsers.add(finalIssue.getProject().getLead().getId());
                            } else {
                                try {
                                    Long id = Long.parseLong(trp.getValue());
                                    matchableUsers.add(userService.getInternal(id).getId());
                                } catch (Exception e) {
                                    // do nothing
                                }
                            }
                        });
                        if (!matchableUsers.contains(currentLogin.getUser().getId())) {
                            throw new JDException("Not allowed to transition", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                        }
                        break;
                    case CONDITION_IS_IN_GROUP:
                        Set<Long> matchableUsersInGroup = new HashSet<>();
                        Arrays.stream(trp.getValue().split(",")).forEach(v -> {
                            try {
                                Optional.of(Long.parseLong(trp.getValue())).flatMap(id -> groupRepo.findById(id)).ifPresent(g -> matchableUsersInGroup.addAll(g.getUsers().stream().map(Login::getId).collect(Collectors.toSet())));
                            } catch (Exception e) {
                                // do nothing
                            }
                        });
                        if (!matchableUsersInGroup.contains(currentLogin.getUser().getId())) {
                            throw new JDException("Not allowed to transition", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                        }
                        break;
                    case CONDITION_HAS_PERMISSION:
                        List<AuthorityCode> codes = new ArrayList<>();
                        Arrays.stream(trp.getValue().split(",")).forEach(a -> codes.add(AuthorityCode.valueOf(a)));
                        if (trp.getCondition().equalsIgnoreCase("OR")) {
                            if (!authService.hasAnyAuthority(codes)) {
                                throw new JDException("Not allowed to transition", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                            }
                        } else if (trp.getCondition().equalsIgnoreCase("AND")) {
                            if (!authService.hasAllAuthority(codes)) {
                                throw new JDException("Not allowed to transition", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                            }
                        }
                        break;
                    case CONDITION_FIELD_REQUIRED:
                        Arrays.stream(trp.getValue().split(",")).forEach(cf -> {
                            finalIssue.getCustomFields().stream().filter(icf -> icf.getCustomField().getKey().equals(cf)).forEach(icf -> {
                                if ((icf.getCustomField().isMultiple() && icf.getValues().length <= 0) || (!icf.getCustomField().isMultiple() && ObjectUtils.isEmpty(icf.getValue())))
                                    throw new JDException("field '" + icf.getCustomField().getName() + "' is required", ErrorCode.TRANSITION_ERROR, HttpStatus.PRECONDITION_FAILED);
                            });
                            switch (cf.toLowerCase()) {
                                case "resolution":
                                    long resolutionID = 0L;
                                    for (int i = 0; i < fieldToUpdate.length(); i++) {
                                        JSONObject field = fieldToUpdate.getJSONObject(i);
                                        if (field.has("label") && field.has("value") &&
                                                field.getString("label").equalsIgnoreCase("resolution")) {
                                            resolutionID = field.getLong("value");
                                        }
                                    }
                                    if (resolutionID <= 0L) {
                                        JSONObject resp = new JSONObject();
                                        resp.put("success", false);
                                        JSONObject field = new JSONObject();
                                        field.put("field", "Resolution");
                                        field.put("current", finalIssue.getResolution());
                                        field.put("values", HelperUtil.squiggly("base", resolutionService.getAll()));
                                        resp.put("fields", new JSONArray().put(field));
                                        throw new JDTransitionException(resp.toString(), ErrorCode.TRANSITION_ERROR, HttpStatus.PRECONDITION_FAILED);
                                    } else {
                                        resolutionService.findById(resolutionID).ifPresent(finalIssue::setResolution);
                                        finalIssue.setResolvedDate(LocalDate.now());
                                    }
                                    break;
                                //throw new JDException("Resolution is required", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                                case "priority":
                                    if (finalIssue.getPriority() == null)
                                        throw new JDException("Priority is required", ErrorCode.TRANSITION_ERROR, HttpStatus.PRECONDITION_FAILED);
                                    break;
                                case "component":
                                    if (finalIssue.getComponents() == null || finalIssue.getComponents().isEmpty())
                                        throw new JDException("Components is required", ErrorCode.TRANSITION_ERROR, HttpStatus.PRECONDITION_FAILED);
                                    break;
                                case "version":
                                    if (finalIssue.getVersions() == null || finalIssue.getVersions().isEmpty())
                                        throw new JDException("Versions is required", ErrorCode.TRANSITION_ERROR, HttpStatus.PRECONDITION_FAILED);
                                    break;
                            }
                        });
                        break;
                    case CONDITION_CHECKLIST_COMPLETE:
                        if (trp.getValue().equalsIgnoreCase("-all-") && taskRepo.countAllByIssueAndCompletedFalse(finalIssue) > 0) {
                            throw new JDException("All checklist items are required to be completed", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                        }
                        break;
                }
                issues.set(0, finalIssue);
            });
            wp.stream().filter(wpp -> wpp.getSubType().toString().startsWith("POST_FUNCTION")).forEach(trp -> {
                switch (trp.getSubType()) {
                    case POST_FUNCTION_UPDATE_FIELD:
                        switch (trp.getKey()) {
                            case "Resolution":
                                if (trp.getValue().equals("-none-")) {
                                    finalIssue.setResolution(null);
                                    finalIssue.setResolvedDate(null);
                                } else {
                                    try {
                                        resolutionService.findById(Long.parseLong(trp.getValue())).ifPresent(finalIssue::setResolution);
                                        finalIssue.setResolvedDate(LocalDate.now());
                                    } catch (Exception e) {
                                        // do nothing
                                    }
                                }
                                break;
                            case "Priority":
                                if (trp.getValue().equals("-none-")) {
                                    finalIssue.setPriority(null);
                                } else {
                                    Priority p = Priority.valueOf(trp.getValue());
                                    if (null != p) {
                                        finalIssue.setPriority(p);
                                    }
                                }
                                break;
                        }
                        break;
                    case POST_FUNCTION_ASSIGN_TO_USER:
                        switch (trp.getValue()) {
                            case "-none-":
                                finalIssue.setAssignee(null);
                                break;
                            case "-current-":
                                finalIssue.setAssignee(userService.getInternal(currentLogin.getUser().getId()));
                                break;
                            case "-reporter-":
                                Login l = userService.getInternal(finalIssue.getReporter().getId());
                                if (l.isActive())
                                    finalIssue.setAssignee(l);
                                break;
                            default:
                                try {
                                    Long id = Long.parseLong(trp.getValue());
                                    Login lo = userService.getInternal(id);
                                    if (authService.getProjectMembersbyAuthority(trp.getTransition().getWorkflow().getProject(), AuthorityCode.PROJECT_VIEW).stream().anyMatch(m -> m.getId().equals(lo.getId())))
                                        finalIssue.setAssignee(lo);
                                } catch (Exception e) {
                                    System.out.println("Unable to assign to user :" + e.getMessage());
                                    // do nothing
                                    e.printStackTrace();
                                }
                                break;
                        }
                        break;
                }
                issues.set(0, finalIssue);
            });
            String oldStep = issue.getCurrentStep().getIssueStatus().getName();
            issue = issues.get(0);
            issue.setCurrentStep(tr.get().getToStep());
            issue = save(issue);
            logHistory(IssueEventType.UPDATE, issue, "status", "transitioned issue", oldStep, issue.getCurrentStep().getIssueStatus().getName());
        }
        if (canTransitionIssue(issue))
            issue.setPossibleTransitions(workflowService.getPossibleTransitions(issue.getIssueType().getWorkflow(), issue.getCurrentStep()));
        issue.setPrevUpdated(prevUpdate);
        return issue;
    }

    public void laneTransition(Long issueKey, String projectKey, Long laneID) {
        Issue issue = getMinimal(projectKey, issueKey);
        Lane lane = boardService.getLane(laneID);
        if (!canTransitionIssue(issue) || null == lane)
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        //Trigger custom field validation
        validationCustomFields(issue);
        Set<WorkflowTransition> possibleTransitionsForIssue = workflowService.getPossibleTransitions(issue.getIssueType().getWorkflow(), issue.getCurrentStep());
        List<WorkflowTransition> possibleTransitions = possibleTransitionsForIssue.stream().filter(pt -> lane.getStatuses().contains(pt.getToStep().getIssueStatus())).collect(Collectors.toList());
        if (possibleTransitions.isEmpty())
            throw new JDException("Transition not available", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        else if (possibleTransitions.size() == 1)
            transition(issueKey, projectKey, possibleTransitions.get(0).getId(), new JSONArray().toString());
        else
            throw new JDException("Multiple possible transitions", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
    }

    @Cacheable(value = "attachments", key = "#projectKey + #issueKey")
    public Set<Attachment> getAttachments(String projectKey, Long issueKey) {
        Issue issue = getIssueMinimal(projectKey, issueKey);
        return attachmentRepo.findByIssue(issue).stream().peek(a -> {
            if (!ObjectUtils.isEmpty(a.getType()) && a.getType().startsWith("image")) a.setPreviewable(true);
            a.setLocation("/issue/" + projectKey + "/" + issueKey + "/attachment/preview/" + a.getName());
        }).collect(Collectors.toSet());
    }

    @Cacheable(value = "issueWorkflow", key = "#projectKey + #issueKey")
    public Workflow getIssueWorkflow(String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (null == issue) throw new JDException("Issue not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return workflowService.findWorkflow(issue.getIssueType().getWorkflow().getId()).get();
    }

    @Cacheable(value = "attachment", key = "#projectKey + #issueKey + #attachmentID")
    public ResponseEntity<Resource> getAttachment(String projectKey, Long issueKey, Long attachmentID) throws
            IOException {
        Issue issue = getIssueMinimal(projectKey, issueKey);
        Attachment a = attachmentRepo.findByIssueAndId(issue, attachmentID);
        if (null == a) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        File file = new File(helperUtil.getDataPath(uploadPath) + a.getName());

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + a.getOriginalName());
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }

    public ResponseEntity<byte[]> getAttachmentPreview(String projectKey, Long issueKey, String attachmentName) {
        Issue issue = getIssueMinimal(projectKey, issueKey);
        if (!canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Attachment a = attachmentRepo.findByIssueAndName(issue, attachmentName);
        if (null == a) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        String ext = FilenameUtils.getExtension(attachmentName);
        if (!ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("gif")
                && !ext.equalsIgnoreCase("pdf")
                && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg")) {
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        byte[] fileContent;
        try {
            fileContent = FileUtils.readFileToByteArray(new File(helperUtil.getDataPath(uploadPath) + a.getName()));
        } catch (IOException e) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        MediaType mediaType = MediaType.IMAGE_JPEG;
        switch (ext) {
            case "png":
                mediaType = MediaType.IMAGE_PNG;
                break;
            case "gif":
                mediaType = MediaType.IMAGE_GIF;
                break;
            case "pdf":
                mediaType = MediaType.APPLICATION_PDF;
                break;
        }
        return ResponseEntity.ok().contentType(mediaType).body(fileContent);
    }

    @CachePut(value = "attachment", key = "#projectKey + #issueKey + #result.name")
    @CacheEvict(value = "attachments", key = "#projectKey + #issueKey")
    @Transactional
    public Attachment saveAttachment(MultipartFile file, String projectKey, Long issueKey) throws IOException {
        Issue issue = getIssueMinimal(projectKey, issueKey);
        if (!canAttachment(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        // Get the file and save it somewhere
        byte[] bytes = file.getBytes();
        String name = projectKey + "" + issueKey + "-" + new Date().getTime();
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        Path path = Paths.get(helperUtil.getDataPath(uploadPath) + name + "." + ext);
        Files.write(path, bytes);
        Attachment a = new Attachment();
        if (ext.equalsIgnoreCase("png") | ext.equalsIgnoreCase("jpg") | ext.equalsIgnoreCase("jpeg")) {
            File file2 = new File(helperUtil.getDataPath(uploadPath) + name + "." + ext);
            Thumbnails.of(file2).size(100, 100).toFile(new File(helperUtil.getDataPath(uploadPath) + "t_" + name + "." + ext));
            a.setThumbnail("t_" + name + "." + ext);
        }
        a.setIssue(issue);
        a.setName(name + "." + ext);
        a.setOriginalName(file.getOriginalFilename());
        a.setSize(file.getSize());
        a.setType(URLConnection.guessContentTypeFromName(file.getOriginalFilename()));
        attachmentRepo.save(a);
        logHistory(IssueEventType.ATTACH, issue, "attachment", "attached", "", a.getOriginalName());
        a.setLocation("/issue/" + projectKey + "/" + issueKey + "/attachment/preview/" + a.getName());
        return a;
    }

    @Caching(evict = {
            @CacheEvict(value = "attachment", key = "#projectKey + #issueKey"),
            @CacheEvict(value = "attachments", key = "#projectKey + #issueKey")
    })
    @Transactional
    public void deleteAttachment(Attachment attachment, String projectKey, Long issueKey) {
        Issue issue = getIssueMinimal(projectKey, issueKey);
        if (attachment.getId() != null && !canDeleteAttachment(issue.getProject(), attachment))
            throw new JDException("Cannot delete", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        try {
            File file = new File(helperUtil.getDataPath(uploadPath) + attachment.getName());
            file.delete();
            if (!attachment.getThumbnail().isEmpty()) {
                file = new File(helperUtil.getDataPath(uploadPath) + attachment.getThumbnail());
                file.delete();
            }
        } catch (Exception e) {
            System.out.println("File not found :" + e.getMessage());
            //do nothing
        }
        String a = attachment.getOriginalName();
        try {
            attachmentRepo.delete(attachment);
        } catch (Exception e) {
            throw new JDException("Error deleting attachment " + attachment.getOriginalName(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        logHistory(IssueEventType.ATTACH_DELETE, issue, "attachment", "removed attachment", a, "");
    }

    @Cacheable(value = "tasks", key = "#projectKey + #issueKey")
    public Set<Task> getTasks(String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (null == issue) throw new JDException("Issue not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return taskRepo.findAllByIssue(issue);
    }

    @CacheEvict(value = "tasks", key = "#projectKey + #issueKey")
    @Transactional
    public Task saveTask(Task task, String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (task.getId() == null && !canEdit(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        AtomicReference<String> oldVal = new AtomicReference<>("");
        //check for old val
        boolean newTask = task.getId() == null;
        if (!newTask) {
            taskRepo.findById(task.getId()).ifPresent(c -> oldVal.set(c.getSummary()));
        }
        task.setIssue(issue);
        taskRepo.save(task);
        logHistory(IssueEventType.UPDATE, issue, "task", newTask ? "added task" : "changed task", oldVal.get(), task.getSummary());
        return task;
    }

    @CacheEvict(value = "tasks", key = "#projectKey + #issueKey")
    public Task completeTask(Task task, String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        Optional<Task> oldTask = taskRepo.findById(task.getId());
        if (oldTask.isEmpty() || !canEdit(issue.getProject())) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        oldTask.get().setCompleted(task.isCompleted());
        oldTask.get().setCompletedDate(task.getCompletedDate());
        taskRepo.save(oldTask.get());
        logHistory(IssueEventType.UPDATE, issue, "task", task.isCompleted() ? "completed task" : "reverted task", "", task.getSummary());
        return task;
    }

    @Transactional
    @CacheEvict(value = "tasks", key = "#projectKey + #issueKey")
    public void reorderTask(List<Task> tasks, String projectKey, Long issueKey) {
        getMinimal(projectKey, issueKey);
        tasks.forEach(t -> taskRepo.changeTaskOrder(t.getId(), t.getTaskOrder()));
    }

    @CacheEvict(value = "tasks", key = "#projectKey + #issueKey")
    @Transactional
    public void deleteTask(Task task, String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (task.getId() != null && !canEdit(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        String commentText = task.getSummary();
        taskRepo.delete(task);
        logHistory(IssueEventType.UPDATE, issue, "task", "removed task", commentText, "");
    }

    @Cacheable(value = "comments", key = "#projectKey + #issueKey")
    public Set<Comment> getComments(String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
        if (null == issue) throw new JDException("Issue not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<Login> members = projectService.getMembersByProjectKey(projectKey);
        return commentRepo.findByIssueOrderByCreatedDateDesc(issue).stream().peek(comment -> {
            if (canManage(issue.getProject())) {
                comment.setEditable(true);
                comment.setDeletable(true);
            } else {
                comment.setEditable(canEditComment(issue.getProject(), comment));
                comment.setDeletable(canDeleteComment(issue.getProject(), comment));
            }
            //comment.setComment(replaceInsertsGET(issue, comment.getComment(), members, attachmentRepo.findByIssue(issue)));
//            Pattern p = Pattern.compile(helperUtil.getUserMentionMatcherRegex());
//            Matcher m = p.matcher(comment.getComment());
//            while (m.find()) {
//                String match = m.group();
//                Optional<Login> matched = members.stream().filter(l -> l.getUserName().equalsIgnoreCase(match.substring(1))).findAny();
//                if (matched.isPresent()) {
//                    comment.setComment(m.replaceFirst("<a class='mention' data-user='" + match.substring(1) + "'>" + matched.get().getFullName() + "</a>"));
//                } else {
//                    comment.setComment(m.replaceFirst("@" + match.substring(1)));
//                }
//                m = p.matcher(comment.getComment());
//            }
        }).sorted(Comparator.comparing(Comment::getCreatedDate)).collect(Collectors.toSet());
    }

    @CachePut(value = "comment", key = "#projectKey + #issueKey")
    @CacheEvict(value = "comments", key = "#projectKey + #issueKey")
    @Transactional
    public Comment saveComment(Comment comment, String projectKey, Long issueKey) {
        Issue issue = getIssueMinimal(projectKey, issueKey);
        if (comment.getId() == null && !canComment(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (comment.getId() != null && !canEditComment(issue.getProject(), comment))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        comment.setBy(currentLogin.getUser());
        if (comment.getId() == null) comment.setCreatedDate(LocalDate.now());
        else comment.setUpdatedDate(LocalDate.now());
        AtomicReference<String> oldVal = new AtomicReference<>("");
        //check for old val
        boolean newComment = comment.getId() == null;
        if (!newComment) {
            commentRepo.findById(comment.getId()).ifPresent(c -> oldVal.set(c.getComment()));
        }
        comment.setIssue(issue);
        //comment.setComment(replaceInsertsPOST(issue, oldVal.get(), comment.getComment(), "comment"));
        commentRepo.save(comment);
        issueAsyncService.trackMentions(issue, oldVal.get(), comment.getComment(), "comment", currentLogin.getUser());
        logHistory(IssueEventType.COMMENT, issue, "comment", newComment ? "commented" : "changed comment", oldVal.get(), comment.getComment());
        return comment;
    }

    @Caching(evict = {
            @CacheEvict(value = "comment", key = "#projectKey + #issueKey"),
            @CacheEvict(value = "comments", key = "#projectKey + #issueKey")
    })
    @Transactional
    public void deleteComment(Comment comment, String projectKey, Long issueKey) {
        Issue issue = getMinimal(projectKey, issueKey);
//        if (!canEdit(issue.getProject()))
//            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
//        if (comment.getId() == null && !canComment(issue.getProject()))
//            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (comment.getId() != null && !canDeleteComment(issue.getProject(), comment))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        String commentText = comment.getComment();
        commentRepo.delete(comment);
        logHistory(IssueEventType.COMMENT_DELETE, issue, "comment", "removed comment", commentText, "");
    }

    public void logHistory(IssueEventType type, Issue issue, String field, String summary, String oldValue, String newValue) {
        issueAsyncService.logHistory(type, issue, field, summary, oldValue, newValue, currentLogin.getUser());
    }

    public List<Resolution> getResolutions() {
        return resolutionService.getAll();
    }

    @Cacheable(value = "watchers", key = "#issue.id")
    public Set<Watchers> getWatchers(Issue issue) {
        return watchersRepo.findByIssue(issue);
    }

    @CacheEvict(value = "watchers", key = "#issue.id")
    public Set<Watchers> addWatcherAndGet(Issue issue, Login watcher) {
        if (null != watcher && watchersRepo.findByIssueAndWatcher(issue, watcher).isEmpty())
            watchersRepo.save(new Watchers(issue, watcher));
        return watchersRepo.findByIssue(issue);
    }

    @CacheEvict(value = "watchers", key = "#issue.id")
    public Set<Watchers> removeWatcherAndGet(Issue issue, Login watcher) {
        if (null != watcher)
            watchersRepo.findByIssueAndWatcher(issue, watcher).forEach(w -> watchersRepo.delete(w));
        return watchersRepo.findByIssue(issue);
    }

    @Cacheable(value = "projectLabels", key = "#projectKey + #q")
    public Set<Label> getLabels(String projectKey, String q) {
        return labelRepo.findByNameLikeIgnoreCase(q);
    }

    @CacheEvict(value = "projectLabels", allEntries = true)
    public Label addLabels(String projectKey, String q) {
        return labelRepo.findByNameLikeIgnoreCase(q).stream().findFirst().orElseGet(() -> labelRepo.save(new Label(null, q)));
    }

    public String getProjectKeyFromPair(String f) {
        return f.substring(0, f.indexOf("-"));
    }

    public long getIssueKeyFromPair(String f) {
        return Long.parseLong(f.substring(f.indexOf("-") + 1));
    }

    public boolean hasGlobalAccess(AuthorityCode code) {
        return authService.hasGlobalAuthority(code);
    }

    public boolean canCreate(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_CREATE);
    }

    public boolean canEdit(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_EDIT);
    }

    public boolean canView(Project p) {
        return projectService.hasProjectViewAccess(p);
    }

    public boolean canManage(Project p) {
        return projectService.hasProjectManageAccess(p);
    }

    public boolean canComment(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.COMMENT_ADD);
    }

    public boolean canEditComment(Project p, Comment c) {
        if (canManage(p)) return true;
        else if (c.getBy().getId().equals(currentLogin.getUser().getId()) && hasAccess(p, AuthorityCode.COMMENT_EDIT_OWN))
            return true;
        else return hasAccess(p, AuthorityCode.COMMENT_EDIT_ALL);
    }

    public boolean canDeleteComment(Project p, Comment c) {
        if (canManage(p)) return true;

        else if (c.getBy().getId().equals(currentLogin.getUser().getId()) && hasAccess(p, AuthorityCode.COMMENT_DELETE_OWN))
            return true;
        else return hasAccess(p, AuthorityCode.COMMENT_DELETE_ALL);
    }

    public boolean canAttachment(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ATTACHMENT_CREATE);
    }

    public boolean canDeleteAttachment(Project p, Attachment c) {
        if (canManage(p)) return true;
        else if (c.getCreatedBy().equals(currentLogin.getUser().getUserName()) && hasAccess(p, AuthorityCode.ATTACHMENT_DELETE_OWN))
            return true;
        else return hasAccess(p, AuthorityCode.ATTACHMENT_DELETE_ALL);
    }

    public boolean canLink(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_LINK);
    }

    public boolean canTransitionIssue(Issue issue) {
        return canTransition(issue.getProject()) || issue.getAssignee().getId().equals(currentLogin.getUser().getId());
    }

    public boolean canTransition(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_TRANSITION);
    }

    public boolean canChangeReporter(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_MODIFY_REPORTER);
    }

    public boolean canChangeAssignee(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_ASSIGN);
    }

    public boolean canDelete(Project p) {
        return canManage(p) || hasAccess(p, AuthorityCode.ISSUE_DELETE);
    }

    public boolean hasAccess(Project p, AuthorityCode code) {
        return authService.hasGlobalAuthority(code) || authService.hasAuthorityForProject(p, code);
    }

    public void reindexAll() {
        issueEventService.reindexAll();
    }
}

@Data
@AllArgsConstructor
class IssueUpdate {
    private boolean success;
    private Date prevUpdate, lastUpdated;
    private Issue issue;

    public IssueUpdate(boolean success, Date prevUpdate, Date lastUpdated) {
        this.success = success;
        this.prevUpdate = prevUpdate;
        this.lastUpdated = lastUpdated;
    }
}