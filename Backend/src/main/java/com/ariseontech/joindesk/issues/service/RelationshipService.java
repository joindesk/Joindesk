package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.IssueMentionsRepo;
import com.ariseontech.joindesk.issues.repo.IssueOtherRelationshipRepo;
import com.ariseontech.joindesk.issues.repo.IssueRelationshipRepo;
import com.ariseontech.joindesk.issues.repo.RelationshipRepo;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RelationshipService {

    @Autowired
    private RelationshipRepo relationshipRepo;
    @Autowired
    private IssueRelationshipRepo issueRelationshipRepo;
    @Autowired
    private IssueOtherRelationshipRepo issueOtherRelationshipRepo;
    @Autowired
    private IssueService issueService;
    @Autowired
    private IssueMentionsRepo issueMentionsRepo;
    @Autowired
    private Validator validator;

    public List<Relationship> getAll() {
        return relationshipRepo.findAll();
    }

    public long count() {
        return relationshipRepo.count();
    }

    public Set<IssueRelationship> getByIssue(Issue issue) {
        if (!issueService.canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueRelationshipRepo.findByFromIssueOrToIssue(issue, issue);
    }

    public Set<IssueOtherRelationship> getOthersByIssue(Issue issue) {
        if (!issueService.canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueOtherRelationshipRepo.findByIssue(issue);
    }

    void deleteByIssue(Issue issue) {
        getByIssue(issue).forEach(ir -> issueRelationshipRepo.delete(ir));
        getMentionsByIssue(issue).forEach(im -> issueMentionsRepo.delete(im));
    }

    public Set<IssueMentions> getMentionsByIssue(Issue issue) {
        if (!issueService.canView(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueMentionsRepo.findByIssue(issue);
    }

    public Relationship saveRelationship(Relationship relationship) {
        Set<ConstraintViolation<Relationship>> result = validator.validate(relationship);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        return relationshipRepo.save(relationship);
    }

    public IssueRelationship saveIssueRelationship(IssueRelationship issueRelationship) {
        Issue fromIssue = null;
        try {
            fromIssue = issueService.get(issueService.getProjectKeyFromPair(issueRelationship.getFromIssuePair()), issueService.getIssueKeyFromPair(issueRelationship.getFromIssuePair()));
        } catch (Exception ignored) {
        }
        if (null == fromIssue) {
            throw new JDException("Invalid Issue", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        Issue toIssue = null;
        try {
            toIssue = issueService.get(issueService.getProjectKeyFromPair(issueRelationship.getToIssuePair()), issueService.getIssueKeyFromPair(issueRelationship.getToIssuePair()));
        } catch (Exception e) {
            throw new JDException("Invalid Issue", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (null == toIssue) {
            throw new JDException("Invalid Issue", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (fromIssue.equals(toIssue)) {
            throw new JDException("Cannot be related to same issue", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        issueRelationship.setFromIssue(fromIssue);
        issueRelationship.setToIssue(toIssue);
        Set<ConstraintViolation<IssueRelationship>> result = validator.validate(issueRelationship);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (!issueService.canLink(issueRelationship.getFromIssue().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueRelationshipRepo.findByTypeAndFromIssueAndToIssue(issueRelationship.getType(), issueRelationship.getFromIssue(), issueRelationship.getToIssue()).stream().findAny().ifPresent(ir -> {
            throw new JDException("", ErrorCode.DUPLICATE, HttpStatus.PRECONDITION_FAILED);
        });
        return issueRelationshipRepo.save(issueRelationship);
    }

    public void removeIssueRelationship(IssueRelationship issueRelationship) {
        Optional<IssueRelationship> ir = issueRelationshipRepo.findById(issueRelationship.getId());
        if (!ir.isPresent()) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        issueRelationship = ir.get();
        Set<ConstraintViolation<IssueRelationship>> result = validator.validate(issueRelationship);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (!issueService.canLink(issueRelationship.getFromIssue().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueRelationshipRepo.delete(issueRelationship);
    }

    public IssueOtherRelationship saveIssueOtherRelationship(String projectKey, Long issueKey, IssueOtherRelationship issueRelationship) {
        Issue issue = null;
        try {
            issue = issueService.get(projectKey, issueKey);
        } catch (Exception ignored) {
        }
        if (null == issue) {
            throw new JDException("Invalid Issue", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        issueRelationship.setIssue(issue);
        Set<ConstraintViolation<IssueOtherRelationship>> result = validator.validate(issueRelationship);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (!issueService.canLink(issue.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return issueOtherRelationshipRepo.save(issueRelationship);
    }

    public void removeIssueOtherRelationship(IssueOtherRelationship issueRelationship) {
        Optional<IssueOtherRelationship> ir = issueOtherRelationshipRepo.findById(issueRelationship.getId());
        if (ir.isEmpty()) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        issueRelationship = ir.get();
        Set<ConstraintViolation<IssueOtherRelationship>> result = validator.validate(issueRelationship);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        if (!issueService.canLink(issueRelationship.getIssue().getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        issueOtherRelationshipRepo.delete(issueRelationship);
    }

    public void removeRelationShip(Relationship relationship) {
        Optional<Relationship> r = relationshipRepo.findById(relationship.getId());
        if (!r.isPresent()) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        if (issueRelationshipRepo.findByType(relationship).isEmpty()) {
            relationshipRepo.delete(r.get());
        } else {
            throw new JDException("Relationship is active", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    public void addLink(String url, String issueKeyPair) {
        Issue issue = issueService.get(issueService.getProjectKeyFromPair(issueKeyPair), issueService.getIssueKeyFromPair(issueKeyPair));
        if (issue != null) {
            if (issueMentionsRepo.findByIssue(issue).stream().noneMatch(im -> im.isLink() && im.getLinkURL().equalsIgnoreCase(url))) {
                issueMentionsRepo.save(new IssueMentions(null, issue, true, false, url));
            }
        }
    }

    public void addMention(String url, String issueKeyPair) {
        Issue issue = issueService.get(issueService.getProjectKeyFromPair(issueKeyPair), issueService.getIssueKeyFromPair(issueKeyPair));
        if (issue != null) {
            if (issueMentionsRepo.findByIssue(issue).stream().noneMatch(im -> im.isMention() && im.getLinkURL().equalsIgnoreCase(url))) {
                issueMentionsRepo.save(new IssueMentions(null, issue, false, true, url));
            }
        }
    }
}
