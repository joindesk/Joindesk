package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.Resolution;
import com.ariseontech.joindesk.issues.repo.ResolutionRepo;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResolutionService {

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private IssueService issueService;

    @Autowired
    private Validator validator;

    public List<Resolution> getAll() {
        return resolutionRepo.findAll().stream().sorted(Comparator.comparing(Resolution::getName)).collect(Collectors.toList());
    }

    @CacheEvict(value = "resolution", allEntries = true)
    public Resolution saveResolution(Resolution resolution) {
        if (resolution.getName().equalsIgnoreCase("unresolved"))
            throw new JDException("Invalid name", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        Set<ConstraintViolation<Resolution>> result = validator.validate(resolution);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        return resolutionRepo.save(resolution);
    }

    @Cacheable(value = "resolution")
    public Optional<Resolution> findById(long l) {
        return resolutionRepo.findById(l);
    }
}
