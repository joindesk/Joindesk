package com.ariseontech.joindesk.issues.repo;


import com.ariseontech.joindesk.issues.domain.FilteredIssueDTO;
import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueFilterDTO;
import com.ariseontech.joindesk.issues.domain.Version;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class IssueFilteringRepo {

    @PersistenceContext
    private EntityManager entityManager;

    public FilteredIssueDTO filter(IssueFilterDTO filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Issue> q = cb.createQuery(Issue.class);
        Root<Issue> issue = q.from(Issue.class);
        List<Predicate> predicates = new ArrayList<Predicate>();
        switch (filter.getFilter().getSortBy()) {
            case "Newest First":
                q.orderBy(cb.desc(issue.get("created")));
                break;
            case "Last Updated":
                q.orderBy(cb.desc(issue.get("updated")));
                break;
        }
        Expression exp;
        if (filter.selected.contains("priority") && null != filter.getPriority() && !filter.getPriority().isEmpty()) {
            exp = issue.get("priority");
            predicates.add(exp.in(filter.getPriority()));
        }
        if (filter.selected.contains("assignee") && null != filter.getAssignee() && !filter.getAssignee().isEmpty()) {
            exp = issue.get("assignee");
            Predicate assigneeP = cb.and(exp.in(filter.getAssignee().stream().filter(r -> r.getId() != null).collect(Collectors.toSet())));
            if (filter.getAssignee().stream().anyMatch(r -> r.getId() == null)) {
                Predicate nullAssigneeP = cb.and(exp.isNull());
                if (filter.getAssignee().size() > 1)
                    predicates.add(cb.or(nullAssigneeP, assigneeP));
                else
                    predicates.add(nullAssigneeP);
            } else {
                predicates.add(assigneeP);
            }
        }
        if (filter.selected.contains("reporter") && null != filter.getReporter() && !filter.getReporter().isEmpty()) {
            exp = issue.get("reporter");
            predicates.add(exp.in(filter.getReporter()));
        }
        if (filter.selected.contains("type") && null != filter.getIssueType() && !filter.getIssueType().isEmpty()) {
            exp = issue.get("issueType");
            predicates.add(exp.in(filter.getIssueType()));
        }
        if (filter.selected.contains("resolution") && null != filter.getResolutions() && !filter.getResolutions().isEmpty()) {
            exp = issue.get("resolution");
            Predicate resolutionP = cb.and(exp.in(filter.getResolutions().stream().filter(r -> r.getId() != null).collect(Collectors.toSet())));
            if (filter.getResolutions().stream().anyMatch(r -> r.getId() == null)) {
                Predicate nullResolutionP = cb.and(exp.isNull());
                if (filter.getResolutions().size() > 1)
                    predicates.add(cb.or(nullResolutionP, resolutionP));
                else
                    predicates.add(nullResolutionP);
            } else {
                predicates.add(resolutionP);
            }
        }
        if (filter.selected.contains("versions") && null != filter.getVersions()) {
            List<Predicate> versionPredicates = new ArrayList<>();
            Join<Issue, Set<Version>> versionResource = issue.join("versions", JoinType.LEFT);
            versionPredicates.add(cb.or(cb.equal(versionResource, filter.getVersions())));
            if (!versionPredicates.isEmpty())
                predicates.add(cb.or(versionPredicates.toArray(new Predicate[0])));
        }
        if (null != filter.getProjects() && !filter.getProjects().isEmpty()) {
            exp = issue.get("project");
            predicates.add(exp.in(filter.getProjects()));
        }
        if (filter.selected.contains("status") && null != filter.getStatus() && !filter.getStatus().isEmpty()) {
            exp = issue.get("currentStep").get("issueStatus");
            predicates.add(exp.in(filter.getStatus()));
        }
        if (filter.selected.contains("created") && null != filter.getCreatedAfter()) {
            predicates.add(cb.greaterThanOrEqualTo(issue.get("created"), filter.getCreatedAfter()));
        }
        if (filter.selected.contains("created") && null != filter.getCreatedBefore()) {
            predicates.add(cb.lessThanOrEqualTo(issue.get("created"), filter.getCreatedBefore()));
        }
        if (filter.selected.contains("updated") && null != filter.getUpdatedAfter()) {
            predicates.add(cb.greaterThanOrEqualTo(issue.get("updated"), filter.getUpdatedAfter()));
        }
        if (filter.selected.contains("updated") && null != filter.getUpdatedBefore()) {
            predicates.add(cb.lessThanOrEqualTo(issue.get("updated"), filter.getUpdatedBefore()));
        }
        if (filter.selected.contains("due") && null != filter.getDueAfter()) {
            predicates.add(cb.greaterThanOrEqualTo(issue.get("dueDate"), filter.getDueAfter()));
        }
        if (filter.selected.contains("due") && null != filter.getDueBefore()) {
            predicates.add(cb.lessThanOrEqualTo(issue.get("dueDate"), filter.getDueBefore()));
        }
        q.where(predicates.toArray(new Predicate[0]));
        TypedQuery<Issue> query = entityManager.createQuery(q);
        query.setFirstResult(Math.toIntExact(filter.getPageIndex() * filter.getPageSize()));
        query.setMaxResults(Math.toIntExact(filter.getPageSize()));
        q.distinct(true);

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.where(predicates.toArray(new Predicate[0]));
        Root<Issue> root = countQuery.from(Issue.class);
        if (filter.selected.contains("versions") && null != filter.getVersions())
            root.join("versions");
        countQuery.select(cb
                .count(root));

        FilteredIssueDTO f = new FilteredIssueDTO();
        f.setIssues(query.getResultList());
        f.setTotal(entityManager.createQuery(countQuery)
                .getSingleResult());
        f.setPageIndex(filter.getPageIndex());
        f.setPageSize(filter.getPageSize());
        return f;
    }

}
