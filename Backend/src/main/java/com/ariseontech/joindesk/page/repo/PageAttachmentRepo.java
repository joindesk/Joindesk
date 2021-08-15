package com.ariseontech.joindesk.page.repo;

import com.ariseontech.joindesk.page.domain.Page;
import com.ariseontech.joindesk.page.domain.PageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface PageAttachmentRepo extends JpaRepository<PageAttachment, Long> {

    Set<PageAttachment> findByPage(Page wiki);

    PageAttachment findByPageAndId(Page wiki, Long id);

    PageAttachment findByPageAndOriginalName(Page wiki, String originalName);

    Set<PageAttachment> findByPageOrderByOriginalNameAsc(Page wiki);
}
