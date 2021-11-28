package com.ariseontech.joindesk.project.service;

import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.ErrorDetails;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.IssueType;
import com.ariseontech.joindesk.issues.repo.IssueCustomRepo;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.issues.service.IssueTypeService;
import com.ariseontech.joindesk.project.domain.CustomField;
import com.ariseontech.joindesk.project.domain.CustomFieldType;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.repo.CustomFieldRepo;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomFieldService {

    @Autowired
    private IssueService issueService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private Validator validator;
    @Autowired
    private CustomFieldRepo customFieldRepo;
    @Autowired
    private IssueTypeService issueTypeService;
    @Autowired
    private IssueCustomRepo issueCustomRepo;

    public Set<CustomField> getAllForProject(String projectKey) {
        Project project = projectService.findByKey(projectKey);
        if (!projectService.hasProjectViewAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return customFieldRepo.findByProjectOrderByNameAsc(project).stream().sorted(Comparator.comparing(CustomField::getName)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    @CacheEvict(value = "issueMinimal", allEntries = true)
    public CustomField save(String projectKey, CustomField customField) {
        boolean isNew = customField.getId() == null;
        Project project = projectService.findByKey(projectKey);
        customField.setProject(project);
        if (!projectService.hasProjectManageAccess(customField.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (isNew) {
            //Remove all white space and switch to lower case for Key
            customField.setKey(customField.getKey().replaceAll("[^a-zA-Z0-9 -]", "").toLowerCase());
            if (!customFieldRepo.findByProjectAndKeyOrderByNameAsc(project, customField.getKey()).isEmpty())
                throw new JDException("Key already taken", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        Set<ConstraintViolation<CustomField>> result = validator.validate(customField);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED);
            error.setErrors(details);
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //If required, will be shown on create by default
        if (customField.isRequired())
            customField.setShowOnCreate(true);
        //Set multiple only for specific fields
        CustomFieldType ty = customField.getType();
        if (customField.isMultiple() && (!ty.equals(CustomFieldType.SELECT) && !ty.equals(CustomFieldType.USER) && !ty.equals(CustomFieldType.VERSION)))
            customField.setMultiple(false);
        //Validation only for Text and number
        if (!customField.getType().equals(CustomFieldType.TEXT) && !customField.getType().equals(CustomFieldType.NUMBER))
            customField.setValidation("");
        //If update, check for few things
        if (customField.getId() != null) {
            CustomField customFieldOrg = customFieldRepo.getOne(customField.getId());
            //Do not allow key change
            customField.setKey(customFieldOrg.getKey());
            if (!customField.getType().equals(customFieldOrg.getType())) {
                //If type is changed, delete all old values
                issueCustomRepo.deleteCustomFieldByKey(customField.getKey());
            }
        }
        //Save only issue types belongs to current project
        List<IssueType> issueTypes = new ArrayList<>();
        customField.getIssueTypes().forEach(itOld -> {
            Optional<IssueType> it = issueTypeService.findOne(itOld.getId());
            if (it.isPresent() && it.get().getProject() != null && it.get().getProject().getKey().equals(projectKey)) {
                issueTypes.add(it.get());
            }
        });
        customField.setIssueTypes(issueTypes);
        customFieldRepo.save(customField);
        if (isNew) {
            customField.getIssueTypes().forEach(it -> issueCustomRepo.updateCustomDataByIssueType(it.getId(),
                    customField.getKey(), "\"" + customField.getDefaultValue() + "\""));
        }
        return customField;
    }

    @Transactional
    public void delete(String projectKey, CustomField customField) {
        if (!projectService.hasProjectManageAccess(customField.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueCustomRepo.deleteCustomFieldByKey(customField.getKey());
        customFieldRepo.delete(customField);
    }


    public CustomField get(String projectKey, Long fieldId) {
        Project project = projectService.findByKey(projectKey);
        if (!projectService.hasProjectManageAccess(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return customFieldRepo.findByProjectAndId(project, fieldId);
    }
}
