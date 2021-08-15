package com.ariseontech.joindesk.project.service;

import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.ErrorDetails;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.repo.ComponentRepo;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ComponentService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private Validator validator;
    @Autowired
    private ComponentRepo componentRepo;

    @Cacheable(value = "component", key = "#projectKey")
    public Set<Component> getAllForProject(String projectKey) {
        Project project = projectService.findByKey(projectKey);
        if (!projectService.hasProjectViewAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return componentRepo.findByProject(project);
    }

    public Set<Component> findAllForProjects(Set<Long> projects) {
        return componentRepo.findAllByProject(projects);
    }

    @CacheEvict(value = "component", key = "#projectKey")
    public Component save(String projectKey, Component component) {
        Project project = projectService.findByKey(projectKey);
        component.setProject(project);
        if (!projectService.hasProjectManageAccess(component.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<ConstraintViolation<Component>> result = validator.validate(component);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED);
            error.setErrors(details);
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        componentRepo.findByProject(project).stream().filter(c -> !c.getId().equals(component.getId()) && c.getName().equalsIgnoreCase(component.getName())).anyMatch(c -> {
            throw new JDException("Duplicate component", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        });
        componentRepo.save(component);
        return component;
    }

    @CacheEvict(value = "component", key = "#projectKey")
    public void delete(String projectKey, Component component) {
        if (!projectService.hasProjectManageAccess(component.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        try {
            componentRepo.delete(component);
        } catch (Exception e) {
            throw new JDException("Component is in use", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    public Component get(String projectKey, Long fieldId) {
        Project project = projectService.findByKey(projectKey);
        if (!projectService.hasProjectManageAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return componentRepo.findByProjectAndId(project, fieldId);
    }
}
