package com.ariseontech.joindesk.wiki.repo;

import com.ariseontech.joindesk.wiki.domain.Wiki;
import com.ariseontech.joindesk.wiki.domain.WikiRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface WikiRevisionRepo extends JpaRepository<WikiRevision, Long> {

    Set<WikiRevision> findByWikiOrderByIdDesc(Wiki wiki);

    WikiRevision findByWikiAndVersion(Wiki wiki, Long version);

    @Query(value = "SELECT MAX(VERSION) FROM WIKI_REVISION WHERE WIKI = ?1", nativeQuery = true)
    Long findLastVersionForWiki(Wiki wiki);
}
