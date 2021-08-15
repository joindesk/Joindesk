package com.ariseontech.joindesk.wiki.repo;

import com.ariseontech.joindesk.wiki.domain.Wiki;
import com.ariseontech.joindesk.wiki.domain.WikiAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface WikiAttachmentRepo extends JpaRepository<WikiAttachment, Long> {

    Set<WikiAttachment> findByWiki(Wiki wiki);

    WikiAttachment findByWikiAndId(Wiki wiki, Long id);

    WikiAttachment findByWikiAndOriginalName(Wiki wiki, String originalName);

    Set<WikiAttachment> findByWikiOrderByOriginalNameAsc(Wiki wiki);
}
