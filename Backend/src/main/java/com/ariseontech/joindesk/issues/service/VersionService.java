package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.IssueFilterDTO;
import com.ariseontech.joindesk.issues.domain.Version;
import com.ariseontech.joindesk.issues.repo.IssueSearchCustomRepo;
import com.ariseontech.joindesk.issues.repo.VersionRepo;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;

@Service
public class VersionService {

    @Autowired
    private VersionRepo versionRepo;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private IssueService issueService;
    @Autowired
    private IssueSearchCustomRepo issueSearchCustomRepo;
    @Autowired
    private Validator validator;

    public Set<Version> getAllVersionForProject(String projectKey) {
        return getAllVersionForProject(projectService.findByKey(projectKey));
    }

    @Cacheable(value = "version")
    public Optional<Version> get(Long id) {
        return versionRepo.findById(id);
    }

    @Cacheable(value = "version", key = "#project.id + '_all'")
    public Set<Version> getAllVersionForProject(Project project) {
        Set<Version> versions = versionRepo.findByProject(project);
        versions.forEach(v -> {
            String q = "resolution is not null AND project = " + project.getId()
                    + " AND ( (data->'version' @> '" + v.getId() + "' ))";
            v.setTotalResolved(issueSearchCustomRepo.count(q).intValue());
            q = "project = " + project.getId()
                    + " AND ( (data->'version' @> '" + v.getId() + "' ))";
            v.setTotalIssues(issueSearchCustomRepo.count(q).intValue());
        });
        return versions;
    }

    public Set<Version> getAllVersionForProjectNoCache(Project project) {
        return versionRepo.findByProject(project);
    }

    @Cacheable(value = "version")
    public Version getVersionForProject(String projectKey, Long versionID) {
        Optional<Version> r = versionRepo.findById(versionID);
        if (r.isEmpty() || !r.get().getProject().getKey().equalsIgnoreCase(projectKey)) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        IssueFilterDTO filter = new IssueFilterDTO();
        filter.setVersions(r.get());
        r.get().setEditable(projectService.hasProjectManageAccess(r.get().getProject()));
        return r.get();
    }

    public Version getVersionForProjectNoCache(Project project, Long versionID) {
        return versionRepo.findByProjectAndId(project, versionID);
    }

    @CacheEvict(value = "version", allEntries = true)
    public Version save(String projectKey, Version v) {
        return save(projectService.findByKey(projectKey), v);
    }

    @CacheEvict(value = "version", allEntries = true)
    public Version save(Project project, Version v) {
        v.setProject(project);
        if (v.getId() == null)
            v.setKey(v.getName().replaceAll("[^a-zA-Z0-9 -]", "").toLowerCase());
        Set<ConstraintViolation<Version>> result = validator.validate(v);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //If already exists
        if (v.getId() != null) {
            Optional<Version> webH = versionRepo.findById(v.getId());
            if (webH.isPresent()) {
                v.setCreated(webH.get().getCreated());
                v.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        return versionRepo.save(v);
    }

    @CacheEvict(value = "version", allEntries = true)
    public Version release(String projectKey, Version v) {
        Optional<Version> vv = versionRepo.findById(v.getId());
        if (vv.isPresent() && vv.get().getProject().getKey().equalsIgnoreCase(projectKey)) {
            boolean release = v.isReleased();
            Date rd = v.getReleaseDate();
            v = vv.get();
            v.setReleased(release);
            v.setReleaseDate(rd);
        } else
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        return versionRepo.save(v);
    }

    @CacheEvict(value = "version", allEntries = true)
    public void remove(String projectID, Version v) {
        remove(projectService.findByKey(projectID), v);
    }

    @CacheEvict(value = "version", allEntries = true)
    public void remove(Project project, Version v) {
        Optional<Version> r = versionRepo.findById(v.getId());
        if (r.isEmpty()) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        try {
            versionRepo.delete(r.get());
        } catch (DataIntegrityViolationException e) {
            throw new JDException("Remove version from issues to delete", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
        }
    }

}
