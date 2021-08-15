package com.ariseontech.joindesk.project.service;

import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.repo.IssueRepo;
import com.ariseontech.joindesk.issues.repo.ReportDTO;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.domain.SlackChannel;
import com.ariseontech.joindesk.project.domain.TimeTracking;
import com.ariseontech.joindesk.project.repo.ProjectRepo;
import com.ariseontech.joindesk.project.repo.TimeTrackingRepo;
import com.ariseontech.joindesk.slack.SlackService;
import com.ariseontech.joindesk.wiki.service.WikiService;
import com.slack.api.methods.SlackApiException;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private Validator validator;
    @Autowired
    private ProjectRepo projectRepo;
    @Autowired
    private IssueRepo issueRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private IssueService issueService;
    @Autowired
    private TimeTrackingRepo timeTrackingRepo;
    @Autowired
    private SlackService slackService;

    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        if (hasGlobalViewAccess()) {
            projectRepo.findAllByOrderByNameAsc().forEach(p -> {
                p.setEditable(hasProjectManageAccess(p));
                projects.add(p);
            });
            return projects;
        }
        projectRepo.findAllByOrderByNameAsc().stream().filter(this::hasProjectManageAccess).forEach(p -> {
            p.setEditable(hasProjectManageAccess(p));
            projects.add(p);
        });
        return projects;
    }

    public List<Project> getViewableProjects() {
        return projectRepo.findAllByOrderByNameAsc().stream().filter(this::hasProjectViewAccess).filter(Project::isActive).peek(p -> {
            p.setEditable(hasProjectManageAccess(p));
            p.setAuthorities(authService.getAllAuthorityForProject(p));
        }).collect(Collectors.toList());
    }

    @CachePut(value = "project", key = "#result.id", unless = "#result != null")
    public Project getProject(Long projectId) {
        Optional<Project> p = projectRepo.findById(projectId);
        if (p.isEmpty())
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        return getProjectDetails(p.get());
    }

    @CachePut(value = "project", key = "#result.id", unless = "#result != null")
    public Project getProjectByKey(String projectKey) {
        Project p = projectRepo.findByKey(projectKey);
        return getProjectDetails(p);
    }

    public List<SlackChannel> getChannels() throws IOException, SlackApiException {
        return slackService.getConversations().stream()
                .map(c -> new SlackChannel(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    @CachePut(value = "project", key = "#projectKey", unless = "#result != null")
    public Project findByKey(String projectKey) {
        Project p = projectRepo.findByKey(projectKey);
        if (null == p)
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectViewAccess(p))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return p;
    }

    private Project getProjectDetails(Project p) {
        if (null == p)
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectViewAccess(p))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        p.setEditable(hasProjectManageAccess(p));
        p.setTimeTracking(getTimeTrackingSettings(p));
        return p;
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public Project createProject(Project project) {
        if (!authService.hasGlobalAuthority(AuthorityCode.PROJECT_MANAGE))
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        project.setLead(project.getLead());
        project.setKey(project.getKey().toUpperCase());
        project.setActive(project.isActive());
        if (projectRepo.findByKey(project.getKey()) != null)
            throw new JDException("", ErrorCode.DUPLICATE_PROJECT_KEY, HttpStatus.PRECONDITION_FAILED);
        projectRepo.save(project);
        return project;
    }

    @Caching(evict = {
            @CacheEvict(value = "projectMembers", allEntries = true),
            @CacheEvict(value = "project", allEntries = true)
    })
    public Project updateProject(Long projectId, Project proj) {
        Optional<Project> p = projectRepo.findById(projectId);
        if (p.isEmpty()) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectManageAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Project project = p.get();
        if (!proj.getName().equals(project.getName()))
            project.setName(proj.getName());
        if (!proj.getDescription().equals(project.getDescription()))
            project.setDescription(proj.getDescription());
        if (proj.isActive() != project.isActive())
            project.setActive(proj.isActive());
        if (proj.getSlackChannel() != project.getSlackChannel())
            project.setSlackChannel(proj.getSlackChannel());
        if (!proj.getLead().getId().equals(project.getLead().getId()))
            project.setLead(proj.getLead());
        if (proj.isNotifyViaSlack() != project.isNotifyViaSlack())
            project.setNotifyViaSlack(proj.isNotifyViaSlack());
        Set<ConstraintViolation<Project>> result = validator.validate(project);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        projectRepo.save(project);
        project.setEditable(true);
        return project;
    }

    public Set<Project> getProjectsOverview() {
        List<Project> projects = getViewableProjects();
        projects.forEach(p -> {
            ReportDTO reportDTO = new ReportDTO();
            reportDTO.addIssueResolutionMap("OPEN", issueRepo.countByProjectAndResolution(p, null));
            reportDTO.addIssueResolutionMap("COMPLETED", issueRepo.countByProjectAndResolutionIsNotNull(p));
            p.setReportDTO(reportDTO);
        });
        return new HashSet<>(projects).stream().sorted(Comparator.comparing(Project::getName)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Cacheable("projectMembers")
    public Set<Login> getMembers(Long projectId) {
        Project p = getProject(projectId);
        return authService.getProjectMembers(p);
    }

    @Cacheable("projectMembers")
    public Set<Login> getMembersByProjectKey(String projectKey) {
        Project p = getProjectByKey(projectKey);
        return authService.getProjectMembers(p);
    }

    @Cacheable("projectTimeTrackingSettings")
    public TimeTracking getTimeTrackingSettings(Project project) {
        TimeTracking tt = timeTrackingRepo.findByProject(project);
        if (null == tt) {
            tt = new TimeTracking(project);
            timeTrackingRepo.save(tt);
        }
        if (!hasProjectViewAccess(tt.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return tt;
    }

    public boolean hasGlobalManageAccess() {
        return authService.hasGlobalAuthority(AuthorityCode.PROJECT_MANAGE);
    }

    public boolean hasGlobalViewAccess() {
        return authService.hasGlobalAuthority(AuthorityCode.PROJECT_VIEW);
    }

    public boolean hasProjectManageAccess(Project p) {
        return hasGlobalManageAccess() || p.isActive() && (authService.hasAuthorityForProject(p, AuthorityCode.PROJECT_MANAGE));
    }

    public boolean hasProjectViewAccess(Project p) {
        return hasProjectManageAccess(p) || p.isActive() && (hasGlobalViewAccess() || authService.hasAuthorityForProject(p, AuthorityCode.PROJECT_VIEW));
    }

    @Caching(evict = {
            @CacheEvict(value = "project", allEntries = true),
            @CacheEvict(value = "projectTimeTrackingSettings", allEntries = true),
    })
    public TimeTracking updateTimeTrackingSettings(TimeTracking timeTracking) {
        Optional<TimeTracking> tt = timeTrackingRepo.findById(timeTracking.getId());
        if (tt.isEmpty()) throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectManageAccess(tt.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        TimeTracking t = tt.get();
        t.setDaysPerWeek(timeTracking.getDaysPerWeek());
        t.setEnabled(timeTracking.isEnabled());
        t.setHoursPerDay(timeTracking.getHoursPerDay());
        t.setTimeFormat(timeTracking.getTimeFormat());
        Set<ConstraintViolation<TimeTracking>> result = validator.validate(t);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        timeTrackingRepo.save(t);
        return t;
    }
}
