package com.ariseontech.joindesk.page.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.page.domain.Page;
import com.ariseontech.joindesk.page.domain.PageAttachment;
import com.ariseontech.joindesk.page.repo.PageRepo;
import com.ariseontech.joindesk.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/page/{projectKey}/", produces = "application/json")
public class PageController {

    @Autowired
    private PageService pageService;

    @Autowired
    private PageRepo pageRepo;

    @RequestMapping(method = RequestMethod.GET, value = "{folderKey}/children")
    public String getWikiFolderChildrens(@PathVariable("projectKey") String projectKey, @PathVariable(value = "folderKey") Long pageFolderKey) {
        return HelperUtil.squiggly("base,pageFolder[page_detail]", pageService.getParentChildrens(projectKey, pageFolderKey));
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getWikis(@PathVariable("projectKey") String projectKey, @RequestParam(value = "pageKey", required = false) Long pageKey) {
        Page page = null;
        if (pageKey != null && pageKey > 0) {
            page = pageRepo.getOne(pageKey);
        }
        return HelperUtil.squiggly("base", pageService.getAllForProject(projectKey, page));
    }

    @RequestMapping(value = "recent", method = RequestMethod.GET)
    public String getRecentWikis(@PathVariable("projectKey") String projectKey) {
        return HelperUtil.squiggly("base,page_detail,audit_details,-content,-content_vector", pageService.getRecentForProject(projectKey));
    }

    @RequestMapping(value = "{pageKey}", method = RequestMethod.GET)
    public String getWiki(@PathVariable("projectKey") String projectKey, @PathVariable(value = "pageKey") Long pageKey, @RequestParam(value = "mode", defaultValue = "view") String mode) {
        if (mode.equalsIgnoreCase("tree"))
            return HelperUtil.squiggly("base,page_detail,audit_details,-content_vector", pageService.getTree(projectKey, pageKey));
        else
            return HelperUtil.squiggly("base,page_detail,audit_details,-content_vector", pageService.getPage(projectKey, pageKey, mode));
    }

    @RequestMapping(value = "search", method = RequestMethod.GET)
    public String searchWiki(@PathVariable("projectKey") String projectKey, @RequestParam(value = "q") String q) {
        return HelperUtil.squiggly("base", pageService.searchPage(projectKey, q));
    }

    @RequestMapping(value = "{pageKey}", method = RequestMethod.POST)
    public String saveWiki(@RequestBody Page page, @PathVariable("projectKey") String projectKey) {
        return HelperUtil.squiggly("base", pageService.savePage(projectKey, page));
    }

    @RequestMapping(value = "{pageKey}/copy", method = RequestMethod.POST)
    public String copyWiki(@RequestBody Page pageFolder, @PathVariable("projectKey") String projectKey
            , @PathVariable("pageKey") Long pageKey) {
        return HelperUtil.squiggly("base", pageService.copyPage(projectKey, pageKey, pageFolder));
    }

    @RequestMapping(value = "{pageKey}/move", method = RequestMethod.POST)
    public String moveWiki(@RequestBody Page pageFolder, @PathVariable("projectKey") String projectKey
            , @PathVariable("pageKey") Long pageKey) {
        return HelperUtil.squiggly("base", pageService.movePage(projectKey, pageKey, pageFolder));
    }

    @RequestMapping(value = "{pageKey}", method = RequestMethod.DELETE)
    public String deleteWiki(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey) {
        pageService.removePage(pageKey);
        return HelperUtil.squiggly("base", "");
    }

    @RequestMapping(method = RequestMethod.GET, value = "{pageKey}/attachments")
    public String getAttachments(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", pageService.getAttachments(projectKey, pageKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{pageKey}/attachment/{attachment_id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getAttachment(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey, @PathVariable("attachment_id") Long attachmentId) throws IOException {
        return pageService.getAttachment(projectKey, pageKey, attachmentId);
    }

    @RequestMapping(value = "{pageKey}/attachment/{attachment_id}/preview", method = RequestMethod.GET)
    public ResponseEntity<byte[]> previewAttachment(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey, @PathVariable("attachment_id") Long attachmentId) {
        return pageService.getAttachmentPreview(projectKey, pageKey, attachmentId);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{pageKey}/attach")
    public String attach(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey, @RequestParam("file") MultipartFile file) throws IOException {
        return HelperUtil.squiggly("base", pageService.saveAttachment(file, projectKey, pageKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{pageKey}/detach")
    public void detach(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey, @RequestBody PageAttachment attachment) {
        pageService.deleteAttachment(attachment, projectKey, pageKey);
    }

    /* REVISION*/
    @RequestMapping(method = RequestMethod.GET, value = "{pageKey}/revisions")
    public String getRevisions(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", pageService.getRevisions(projectKey, pageKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{pageKey}/revisions/{revisionID}")
    public String getRevision(@PathVariable("projectKey") String projectKey, @PathVariable("pageKey") Long pageKey, @PathVariable("revisionID") Long revisionID) {
        return HelperUtil.squiggly("base,page_detail,audit_details", pageService.getRevision(projectKey, pageKey, revisionID));
    }
}
