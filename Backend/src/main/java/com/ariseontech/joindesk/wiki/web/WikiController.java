package com.ariseontech.joindesk.wiki.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.wiki.domain.Wiki;
import com.ariseontech.joindesk.wiki.domain.WikiAttachment;
import com.ariseontech.joindesk.wiki.domain.WikiFolder;
import com.ariseontech.joindesk.wiki.repo.WikiRepo;
import com.ariseontech.joindesk.wiki.service.WikiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/wiki/{projectKey}/", produces = "application/json")
public class WikiController {

    @Autowired
    private WikiService wikiService;

    @Autowired
    private WikiRepo wikiRepo;

    @RequestMapping(method = RequestMethod.GET, value = "folder/{folderKey}")
    public String getWikiFolder(@PathVariable("projectKey") String projectKey, @PathVariable(value = "folderKey") Long wikiFolderKey) {
        return HelperUtil.squiggly("base,wikiFolder[wiki_detail],wikis[audit_details]", wikiService.getFolder(projectKey, wikiFolderKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "folder/{folderKey}/children")
    public String getWikiFolderChildrens(@PathVariable("projectKey") String projectKey, @PathVariable(value = "folderKey") Long wikiFolderKey) {
        return HelperUtil.squiggly("base,wikiFolder[wiki_detail]", wikiService.getFolderChildrens(projectKey, wikiFolderKey));
    }

    @RequestMapping(method = RequestMethod.GET)
    public String getWikis(@PathVariable("projectKey") String projectKey, @RequestParam(value = "wikiKey", required = false) Long wikiKey) {
        Wiki wiki = null;
        if (wikiKey != null && wikiKey > 0) {
            wiki = wikiRepo.getOne(wikiKey);
        }
        return HelperUtil.squiggly("base", wikiService.getAllForProject(projectKey, wiki));
    }

    @RequestMapping(value = "recent", method = RequestMethod.GET)
    public String getRecentWikis(@PathVariable("projectKey") String projectKey) {
        return HelperUtil.squiggly("base", wikiService.getRecentForProject(projectKey));
    }

    @RequestMapping(value = "{wikiKey}", method = RequestMethod.GET)
    public String getWiki(@PathVariable("projectKey") String projectKey, @PathVariable(value = "wikiKey") Long wikiKey, @RequestParam(value = "mode", defaultValue = "view") String mode) {
        return HelperUtil.squiggly("base,wiki_detail,audit_details", wikiService.getWiki(projectKey, wikiKey, mode));
    }

    @RequestMapping(value = "search", method = RequestMethod.GET)
    public String searchWiki(@PathVariable("projectKey") String projectKey, @RequestParam(value = "q") String q) {
        return HelperUtil.squiggly("base", wikiService.searchWiki(projectKey, q));
    }

    @RequestMapping(value = "searchFolder", method = RequestMethod.GET)
    public String searchWikiFolder(@PathVariable("projectKey") String projectKey, @RequestParam(value = "q") String q) {
        return HelperUtil.squiggly("base", wikiService.searchFolder(projectKey, q));
    }

    @RequestMapping(value = "{wikiKey}", method = RequestMethod.POST)
    public String saveWiki(@RequestBody Wiki wiki, @PathVariable("projectKey") String projectKey) {
        return HelperUtil.squiggly("base", wikiService.saveWiki(projectKey, wiki));
    }

    @RequestMapping(value = "{wikiKey}/copy", method = RequestMethod.POST)
    public void copyWiki(@RequestBody WikiFolder wikiFolder, @PathVariable("projectKey") String projectKey
            , @PathVariable("wikiKey") Long wikiKey) {
        wikiService.copyWiki(projectKey, wikiKey, wikiFolder);
    }

    @RequestMapping(value = "folder/{folderKey}/move", method = RequestMethod.POST)
    public void moveWikiFolder(@RequestBody WikiFolder wikiFolder, @PathVariable("projectKey") String projectKey
            , @PathVariable("folderKey") Long folderKey) {
        wikiService.moveWikiFolder(projectKey, folderKey, wikiFolder);
    }

    @RequestMapping(value = "{wikiKey}/move", method = RequestMethod.POST)
    public void moveWiki(@RequestBody WikiFolder wikiFolder, @PathVariable("projectKey") String projectKey
            , @PathVariable("wikiKey") Long wikiKey) {
        wikiService.moveWiki(projectKey, wikiKey, wikiFolder);
    }

    @RequestMapping(value = "folder/{folderKey}", method = RequestMethod.POST)
    public String saveWikiFolder(@RequestBody WikiFolder wikiFolder, @PathVariable("projectKey") String projectKey,
                                 @PathVariable(value = "folderKey") Long wikiFolderKey) {
        return HelperUtil.squiggly("base", wikiService.saveWikiFolder(projectKey, wikiFolderKey, wikiFolder));
    }

    @RequestMapping(value = "folder/{folderKey}/rename", method = RequestMethod.POST)
    public void renameWikiFolder(@RequestBody WikiFolder wikiFolder, @PathVariable("projectKey") String projectKey,
                                 @PathVariable(value = "folderKey") Long wikiFolderKey) {
        wikiService.renameWikiFolder(projectKey, wikiFolder);
    }

    @RequestMapping(value = "{wikiKey}", method = RequestMethod.DELETE)
    public String deleteWiki(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey) {
        wikiService.removeWiki(wikiKey);
        return HelperUtil.squiggly("base", "");
    }

    @RequestMapping(value = "folder/{folderKey}", method = RequestMethod.DELETE)
    public String deleteWikiFolder(@PathVariable("projectKey") String projectKey, @PathVariable("folderKey") Long folderKey) {
        wikiService.removeWikiFolder(folderKey);
        return HelperUtil.squiggly("base", "");
    }

    @RequestMapping(method = RequestMethod.GET, value = "{wikiKey}/attachments")
    public String getAttachments(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", wikiService.getAttachments(projectKey, wikiKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{wikiKey}/attachment/{attachment_id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getAttachment(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey, @PathVariable("attachment_id") Long attachmentId) throws IOException {
        return wikiService.getAttachment(projectKey, wikiKey, attachmentId);
    }

    @RequestMapping(value = "{wikiKey}/attachment/{attachment_id}/preview", method = RequestMethod.GET)
    public ResponseEntity<byte[]> previewAttachment(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey, @PathVariable("attachment_id") Long attachmentId) {
        return wikiService.getAttachmentPreview(projectKey, wikiKey, attachmentId);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{wikiKey}/attach")
    public String attach(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey, @RequestParam("file") MultipartFile file) throws IOException {
        return HelperUtil.squiggly("base", wikiService.saveAttachment(file, projectKey, wikiKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{wikiKey}/detach")
    public void detach(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey, @RequestBody WikiAttachment attachment) {
        wikiService.deleteAttachment(attachment, projectKey, wikiKey);
    }

    /* REVISION*/
    @RequestMapping(method = RequestMethod.GET, value = "{wikiKey}/revisions")
    public String getRevisions(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", wikiService.getRevisions(projectKey, wikiKey));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{wikiKey}/revisions/{revisionID}")
    public String getRevision(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey, @PathVariable("revisionID") Long revisionID) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", wikiService.getRevision(projectKey, wikiKey, revisionID));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{wikiKey}/compare/{sourceRev}/{targetRev}")
    public String compareRevisions(@PathVariable("projectKey") String projectKey, @PathVariable("wikiKey") Long wikiKey,
                                   @PathVariable("sourceRev") Long sourceRev, @PathVariable("targetRev") Long targetRev) {
        return HelperUtil.squiggly("base,issue_detail,audit_details", wikiService.compareRevision(projectKey, wikiKey, sourceRev, targetRev));
    }
}
