package com.ariseontech.joindesk.page.repo;

import com.ariseontech.joindesk.page.domain.Page;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.wiki.domain.Wiki;
import com.ariseontech.joindesk.wiki.domain.WikiFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface PageRepo extends JpaRepository<Page, Long> {

    Set<Page> findFirst10ByProjectOrderByCreatedDesc(Project project);

    Set<Page> findFirst10ByProjectOrderByUpdatedDesc(Project project);

    Set<Page> findByProjectOrderByTitleAsc(Project project);

    Set<Page> findByProjectAndParentOrderByTitleAsc(Project project, Page parent);

    long countByParent(Page parent);

    @Query(value = "Select * from wiki where project = :project AND CONCAT ( LOWER (title), LOWER (content) ) LIKE  %:query%", nativeQuery = true)
    Set<Page> findByContentContainingOrTitleContaining(@Param("query") String q, @Param("project") Long projectID);
}
