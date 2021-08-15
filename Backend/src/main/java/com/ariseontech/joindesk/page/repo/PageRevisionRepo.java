package com.ariseontech.joindesk.page.repo;

import com.ariseontech.joindesk.page.domain.Page;
import com.ariseontech.joindesk.page.domain.PageRevision;
import com.ariseontech.joindesk.wiki.domain.Wiki;
import com.ariseontech.joindesk.wiki.domain.WikiRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.Set;

public interface PageRevisionRepo extends JpaRepository<PageRevision, Long> {

    Set<PageRevision> findByPageOrderByIdDesc(Page wiki);

    @Transactional
    @Modifying
    void deleteByPage(Page wiki);

    PageRevision findByPageAndVersion(Page wiki, Long version);

    @Query(value = "SELECT MAX(VERSION) FROM page_revision WHERE PAGE = ?1", nativeQuery = true)
    Long findLastVersionForPage(Page wiki);
}
