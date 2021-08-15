package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueType;
import com.ariseontech.joindesk.issues.domain.Resolution;
import com.ariseontech.joindesk.issues.domain.WorkflowStep;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface IssueRepo extends JpaRepository<Issue, Long> {

    Set<Issue> findByProject(Project project);

    Long countByProject(Project project);

    Long countByProjectAndResolution(Project project, Resolution resolution);

    Long countByProjectAndResolutionIsNotNull(Project project);

    @Query(nativeQuery = true, value =
            "SELECT v.current_step AS status, COUNT(v) AS cnt FROM issue v where v.project = ?1 GROUP BY v.current_step")
    List<ReportQueryInterfaceDTO> countGroupByCurrentStep(Long id);

    @Query(nativeQuery = true, value =
            "SELECT v.issue_type AS status, COUNT(v) AS cnt FROM issue v where v.project = ?1 GROUP BY v.issue_type")
    List<ReportQueryInterfaceDTO> countGroupByType(Long id);

    @Query(nativeQuery = true, value =
            "SELECT v.resolution AS status, COUNT(v) AS cnt FROM issue v where v.project = ?1 GROUP BY v.resolution")
    List<ReportQueryInterfaceDTO> countGroupByResolution(Long id);

    @Query(nativeQuery = true, value =
            "SELECT v.assignee AS status, COUNT(v) AS cnt FROM issue v where v.project = ?1 GROUP BY v.assignee")
    List<ReportQueryInterfaceDTO> countGroupByAssignee(Long id);

    @Query(nativeQuery = true, value =
            "SELECT v.reporter AS status, COUNT(v) AS cnt FROM issue v where v.project = ?1 GROUP BY v.reporter")
    List<ReportQueryInterfaceDTO> countGroupByReporter(Long id);

    Issue findByKey(Long key);

    Issue findByProjectAndKey(Project project, Long key);

    @Query(nativeQuery = true, value = "select id from issue v where v.project = ?1 and v.key = ?2")
    long findMinimalByProjectAndKey(Project project, Long key);

    @Query(nativeQuery = true, value = "select id from issue v where v.id = ?1")
    long findMinimalByIssueID(Long id);

    @Query(nativeQuery = true, value = "select updated from issue v where v.project = ?1 and v.key = ?2")
    Date findUpdatedByProjectAndKey(Project project, Long key);

    List<Issue> findAllByProjectAndKey(Project project, Long key);

    Set<Issue> findByIssueType(IssueType issueType);

    Set<Issue> findByCurrentStep(WorkflowStep step);

    Set<Issue> findAllByDueDateLessThanEqual(LocalDate date);

    @Query(value = "SELECT MAX(KEY) FROM ISSUE WHERE PROJECT = ?1", nativeQuery = true)
    Long findLastKeyForProject(Project project);

    @Query(value = "SELECT nextval(?)", nativeQuery = true)
    Long findLastKeyBySeq(String projectKey);

    @Query(value = "SELECT * FROM ISSUE WHERE SUMMARY LIKE ?1 OR description LIKE ?2", nativeQuery = true)
    Set<Issue> containsText(String text, String text2);

    @Query(value = "SELECT * FROM ISSUE WHERE (content_vector @@ to_tsquery(:query) OR LOWER (description) LIKE %:query% ) AND PROJECT = :project LIMIT 100", nativeQuery = true)
    Set<Issue> containsTextByProject(@Param("query") String s, @Param("project") Long project);
    
    @Query(value = "SELECT * FROM ISSUE WHERE PROJECT = :project order by id desc LIMIT :limit", nativeQuery = true)
    Set<Issue> getLatestByProject(@Param("project") Long project,@Param("limit") int limit);

}