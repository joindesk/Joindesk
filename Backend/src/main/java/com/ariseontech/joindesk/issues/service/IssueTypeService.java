package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.IssueRepo;
import com.ariseontech.joindesk.issues.repo.IssueStatusRepo;
import com.ariseontech.joindesk.issues.repo.IssueTypeRepo;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IssueTypeService {

    @Autowired
    private IssueTypeRepo issueTypeRepo;
    @Autowired
    private IssueStatusRepo issueStatusRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private IssueRepo issueRepo;
    @Autowired
    private Validator validator;

    public List<IssueType> getAll(String projectKey, boolean includeInactive) {
        Project p = projectService.findByKey(projectKey);
        List<IssueType> issueTypes = new ArrayList<>();
        issueTypeRepo.findByProjectOrderByNameAsc(p).forEach(i -> {
            i.setEditable(canEdit(i.getProject()));
            if (includeInactive)
                issueTypes.add(i);
            else {
                if (i.isActive()) issueTypes.add(i);
            }
        });
        return issueTypes;
    }

    @Cacheable(value = "issueType")
    public Optional<IssueType> findOne(Long issueTypeID) {
        Optional<IssueType> issueType = issueTypeRepo.findById(issueTypeID);
        if (issueType.isEmpty()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectViewAccess(issueType.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueType.get().setEditable(canEdit(issueType.get().getProject()));
        return issueType;
    }

    @Cacheable(value = "issueType")
    public IssueType findByNameAndProject(String name, Project project) {
        if (!projectService.hasProjectViewAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueTypeRepo.findByNameAndProject(name, project);
    }

    public List<Issue> findByWorkflow(Workflow workflow) {
        if (!projectService.hasProjectViewAccess(workflow.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueTypeRepo.findByWorkflowOrderByNameAsc(workflow);
    }

    @CacheEvict(value = "issueType", allEntries = true)
    public IssueType update(Long id, IssueType it) {
        Optional<IssueType> issueType = issueTypeRepo.findById(id);
        if (!issueType.isPresent()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectManageAccess(issueType.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        IssueType issueType1 = issueType.get();
        if (!ObjectUtils.isEmpty(it.getName()))
            issueType1.setName(it.getName());
        if (!ObjectUtils.isEmpty(it.getDescription()))
            issueType1.setDescription(it.getDescription());
        if (it.isActive() != issueType1.isActive())
            issueType1.setActive(it.isActive());
        if (!ObjectUtils.isEmpty(it.getIconUrl())) {
            issueTypeRepo.findByIconUrlAndProject(it.getIconUrl(), issueType.get().getProject())
                    .stream().filter(itt -> !itt.getId().equals(issueType.get().getId())).anyMatch(m -> {
                throw new JDException("Icon already taken", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
            });
            issueType1.setIconUrl(it.getIconUrl());
        }
        return issueTypeRepo.save(issueType1);
    }

    public WorkflowChange changeWorkflow(Long id, Long workflow_id) {
        Optional<IssueType> issueType = issueTypeRepo.findById(id);
        if (issueType.isEmpty()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectManageAccess(issueType.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        IssueType issueType1 = issueType.get();
        Set<Issue> issues = issueRepo.findByIssueType(issueType1);
        WorkflowChange workflowChange = new WorkflowChange();
        Set<WorkflowStep> fromSteps = workflowChange.getFromSteps();
        Set<WorkflowStep> toSteps = workflowChange.getToSteps();
        issues.forEach(i -> {
            fromSteps.add(i.getCurrentStep());
        });
        toSteps.addAll(workflowService.getWorkflowSteps(workflowService.findWorkflow(workflow_id).get()));
        workflowChange.setFromSteps(fromSteps);
        workflowChange.setToSteps(toSteps);
        return workflowChange;
    }

    @CacheEvict(value = "issueType", allEntries = true)
    public IssueType changeWorkflow(Long id, Workflow workflow, WorkflowChange stepMapping) {
        Optional<IssueType> issueType = issueTypeRepo.findById(id);
        if (!issueType.isPresent()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectManageAccess(issueType.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        IssueType issueType1 = issueType.get();
        Set<Issue> issues = issueRepo.findByIssueType(issueType1);
        AtomicBoolean mapNotFound = new AtomicBoolean(false);
        issues.forEach(i -> {
            if (!stepMapping.getMap().containsKey(i.getCurrentStep())) {
                mapNotFound.set(true);
            }
        });
        //If not all mapping solved then throw exception
        if (mapNotFound.get()) {
            throw new JDException("", ErrorCode.DUPLICATE, HttpStatus.PRECONDITION_FAILED);
        }
        issues.forEach(i -> {
            i.setCurrentStep(stepMapping.getMap().get(i.getCurrentStep()));
        });
        issueRepo.saveAll(issues);
        issueType1.setWorkflow(workflow);
        return issueTypeRepo.save(issueType1);
    }

    @CacheEvict(value = "issueType", allEntries = true)
    public void deleteIssueType(Long id) {
        Optional<IssueType> issueType = issueTypeRepo.findById(id);
        if (!issueType.isPresent()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectManageAccess(issueType.get().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (!issueRepo.findByIssueType(issueType.get()).isEmpty())
            throw new JDException("Issue type is associated to issues", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        issueTypeRepo.delete(issueType.get());
    }

    @CacheEvict(value = "issueType", allEntries = true)
    @Transactional
    public IssueType create(String projectKey, IssueType it) {
        Project project = projectService.findByKey(projectKey);
        if (!projectService.hasProjectManageAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        IssueType issueType = new IssueType(it.getName(), it.getIconUrl(), it.getDescription(), project);
        issueType.setWorkflow(it.getWorkflow());
        if (null != it.getWorkflow() && it.getWorkflow().getId() == 0)
            issueType.setWorkflow(workflowService.create(new Workflow(issueType.getName(), project)));
        Set<ConstraintViolation<IssueType>> result = validator.validate(issueType);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        return issueTypeRepo.save(issueType);
    }

    public List<IssueStatus> getAllStatus() {
        return issueStatusRepo.findAll();
    }

    @Cacheable(value = "issueStatus")
    public Optional<IssueStatus> findStatus(Long issueStatusID) {
        return issueStatusRepo.findById(issueStatusID);
    }

    @CacheEvict(value = "issueStatus", allEntries = true)
    public IssueStatus saveStatus(IssueStatus issueStatus) {
        if (issueStatus.getId() == null) {
            issueStatus.setBy(authService.currentLogin());
        }
        //If same name status exists return it as name is unique
        IssueStatus d = issueStatusRepo.findByName(issueStatus.getName());
        if (d != null) {
            return d;
        }
        Set<ConstraintViolation<IssueStatus>> result = validator.validate(issueStatus);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        return issueStatusRepo.save(issueStatus);
    }

    public boolean canView(Project p) {
        return projectService.hasProjectViewAccess(p);
    }

    public boolean canEdit(Project p) {
        return projectService.hasProjectManageAccess(p);
    }

    public boolean canManage(Project p) {
        return projectService.hasProjectManageAccess(p);
    }
}
