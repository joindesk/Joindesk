package com.ariseontech.joindesk.page.service;

import com.ariseontech.joindesk.DiffMatchPatch;
import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.page.domain.*;
import com.ariseontech.joindesk.page.repo.PageAttachmentRepo;
import com.ariseontech.joindesk.page.repo.PageCustomRepo;
import com.ariseontech.joindesk.page.repo.PageRepo;
import com.ariseontech.joindesk.page.repo.PageRevisionRepo;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.ProjectService;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class PageService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PageRepo pageRepo;
    @Autowired
    private PageCustomRepo pageCustomRepo;
    @Autowired
    private PageRevisionRepo pageRevisionRepo;
    @Autowired
    private Validator validator;
    @Autowired
    private PageAttachmentRepo pageAttachmentRepo;
    @Value("${wiki-revision-dir}")
    private String uploadPath;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HelperUtil helperUtil;


    public Set<Page> getAllForProject(String projectKey, Page parent) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        return pageRepo.findByProjectOrderByTitleAsc(project);
    }

    public List<Page> getRecentForProject(String projectKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        Set<Page> recentPages = pageRepo.findFirst10ByProjectOrderByUpdatedDesc(project);
        recentPages.addAll(pageRepo.findFirst10ByProjectOrderByCreatedDesc(project));
        return recentPages.stream().sorted(Comparator.comparing(Page::getUpdated).reversed())
                .collect(Collectors.toList());
    }

    public Page getPage(String projectKey, Long wikiKey, String mode) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        Optional<Page> wiki = pageRepo.findById(wikiKey);
        if (wiki.isEmpty()) throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.get().getProject());
        wiki.get().setEditable(canEdit(project));
        wiki.get().setDeletable(canDelete(project));
        return wiki.get();
    }

    public Page savePage(String projectKey, Page wiki) {
        boolean createRevision = false;
        String oldContent = "";
        String oldTitle = "";
        Login currentLogin = authService.currentLogin();
        Login revisionLogin = currentLogin;
        if (wiki.getId() == null || wiki.getId() <= 0) {
            wiki.setCreatedLogin(currentLogin);
        } else {
            Optional<Page> w = pageRepo.findById(wiki.getId());
            if (w.isEmpty())
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            oldContent = w.get().getContent();
            oldTitle = w.get().getTitle();
            wiki.setParent(w.get().getParent());
            wiki.setCreatedLogin(w.get().getCreatedLogin());
            if (wiki.getLastUpdatedLogin() != null)
                revisionLogin = wiki.getLastUpdatedLogin();
            wiki.setLastUpdatedLogin(currentLogin);
            createRevision = true;
        }
        wiki.setProject(projectService.findByKey(projectKey));
        hasView(wiki.getProject());
        if (!canEdit(wiki.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<ConstraintViolation<Page>> result = validator.validate(wiki);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //If already exists
        if (wiki.getId() != null) {
            Optional<Page> webH = pageRepo.findById(wiki.getId());
            if (webH.isPresent()) {
                wiki.setCreated(webH.get().getCreated());
                wiki.setCreatedBy(webH.get().getCreatedBy());
                wiki.setHasChild(pageRepo.countByParent(wiki) > 0);
            }
        }
        //wiki.setContent(replaceInsertsPOST(wiki));
        wiki = pageRepo.save(wiki);
        pageCustomRepo.update(wiki);
        if (createRevision) {
            Long lastKey = pageRevisionRepo.findLastVersionForPage(wiki);
            lastKey = (null == lastKey) ? 0 : lastKey;
            Long rev = lastKey + 1;
            String file = wiki.getProject().getId() + "-" + wiki.getId() + "-" + rev + "-" + new Date().getTime();
            PageRevision wikiRevision = new PageRevision(wiki, rev, file, revisionLogin);
            wikiRevision.setTitle(oldTitle);
            wikiRevision.setContent(oldContent);
            pageRevisionRepo.save(wikiRevision);
        }
        if (wiki.getParent() != null && !wiki.getParent().isHasChild())
            pageCustomRepo.setHasChild(wiki.getParent(), true);
        return wiki;
    }

    public void removePage(Long wikiKey) {
        Optional<Page> wiki = pageRepo.findById(wikiKey);
        if (wiki.isEmpty()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.get().getProject());
        if (!authService.hasAuthorityForProject(wiki.get().getProject(), AuthorityCode.WIKI_DELETE)) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        removePage(wiki.get());
    }

    private void removePage(Page page) {
        Set<Page> pages = pageRepo.findByProjectAndParentOrderByTitleAsc(page.getProject(), page);
        if (!pages.isEmpty()) {
            pages.forEach(this::removePage);
        }
        //Delete versions
        pageRevisionRepo.deleteByPage(page);
        pageAttachmentRepo.findByPage(page).forEach(wikiAttachment ->
                deleteAttachment(wikiAttachment, wikiAttachment.getPage().getProject().getKey(), wikiAttachment.getPage().getId()));
        pageRepo.delete(page);
    }

    private boolean hasView(Project project) {
        if (!authService.hasAuthorityForProject(project, AuthorityCode.WIKI_VIEW)) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return true;
    }

    private boolean canEdit(Project project) {
        return authService.hasAuthorityForProject(project, AuthorityCode.WIKI_EDIT);
    }

    private boolean canDelete(Project project) {
        return authService.hasAuthorityForProject(project, AuthorityCode.WIKI_DELETE);
    }

    public List<Page> searchPage(String projectKey, String q) {
        //q = q + "*";
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        return pageCustomRepo.filter(q.replaceAll(" ", "|"), project);
        //return wikiLuceneService.searchPage(q.toLowerCase() + " AND project:" + projectKey);
    }

    public Page copyPage(String projectKey, Long wikiKey, Page wikiF) {
        Project project = projectService.findByKey(projectKey);
        if (!canEdit(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<Page> wiki = pageRepo.findById(wikiKey);
        if (wiki.isEmpty()) throw new JDException("Page not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Optional<Page> parentPage = pageRepo.findById(wikiF.getId());
        if (parentPage.isEmpty())
            throw new JDException("Parent not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Page wikiCopy = new Page();
        wikiCopy.setParent(parentPage.get());
        wikiCopy.setTitle("Copy of " + wiki.get().getTitle());
        wikiCopy.setContent(wiki.get().getContent());
        wikiCopy.setProject(wiki.get().getProject());
        return savePage(projectKey, wikiCopy);
    }

    public Page movePage(String projectKey, Long wikiKey, Page wikiF) {
        Project project = projectService.findByKey(projectKey);
        if (!canEdit(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<Page> wiki = pageRepo.findById(wikiKey);
        if (wiki.isEmpty()) throw new JDException("Page not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Optional<Page> parentPage = pageRepo.findById(wikiF.getId());
        if (parentPage.isEmpty())
            throw new JDException("Parent not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Page w = wiki.get();
        Page prevParent = w.getParent();
        w.setParent(parentPage.get());
        w = pageRepo.save(w);
        if (prevParent != null) {
            pageCustomRepo.setHasChild(prevParent, pageRepo.countByParent(prevParent) > 0);
        }
        pageCustomRepo.setHasChild(w.getParent(), pageRepo.countByParent(w.getParent()) > 0);
        return w;
    }

    public PageData getParent(String projectKey, Long wikiFolderKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        PageData data = new PageData();
        Optional<Page> parentFolder = pageRepo.findById(wikiFolderKey);
        parentFolder.ifPresent(wikiFolder -> {
            wikiFolder.setEditable(canEdit(project));
            wikiFolder.setDeletable(wikiFolder.getParent() != null && canDelete(project));
            data.setPageParent(wikiFolder);
            data.setPageParents(pageRepo.findByProjectAndParentOrderByTitleAsc(project, wikiFolder));
            data.setPages(data.getPageParents());
            data.setPath(getPath(projectKey, wikiFolderKey));
            data.setTree(getTreePath(projectKey, wikiFolderKey, data.getPath()));
        });
        return data;
    }

    public List<PagePath> getParentChildrens(String projectKey, Long wikiFolderKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        Optional<Page> parentFolder = pageRepo.findById(wikiFolderKey);
        return parentFolder.map(wikiFolder -> getChildren(project, wikiFolder, 0L, new ArrayList<>())).orElseGet(ArrayList::new);
    }

    private List<Page> getPath(String projectKey, Long wikiFolderKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        List<Page> folders = new ArrayList<>();
        Optional<Page> folder = pageRepo.findById(wikiFolderKey);
        if (folder.isPresent() && folder.get().getParent() != null) {
            do {
                folder = pageRepo.findById(folder.get().getParent().getId());
                folders.add(folder.get());
            }
            while (folder.get().getParent() != null);
        }
        return folders;
    }

    private List<PagePath> getChildren(Project project, Page wikiFolder, Long selectFolderKey, List<String> pathsKeys) {
        List<PagePath> childrens = new ArrayList<>();
        pageRepo.findByProjectAndParentOrderByTitleAsc(project, wikiFolder).forEach(wf -> {
            PagePath wp = new PagePath(wf.getTitle(), wf.getId().toString());
            wp.setParent(wf);
            if (pathsKeys.contains(wp.getKey()))
                wp.setExpanded(true);
            if (wf.getId().equals(selectFolderKey))
                wp.setSelected(true);
            wp.setLeaf(!wf.isHasChild());
            if (wp.isLeaf())
                wp.setIcon("anticon anticon-file");
            if (wp.isExpanded() || wp.isSelected())
                wp.setChildren(getChildren(project, wf, selectFolderKey, pathsKeys));
            childrens.add(wp);
        });
        return childrens;
    }

    public List<PagePath> getTreePath(String projectKey, Long wikiFolderKey, List<Page> path) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        Page root;
        if (path.isEmpty()) {
            root = pageRepo.findById(wikiFolderKey).get();
        } else {
            root = (Page) new ReverseListIterator(path).next();
        }
        PagePath r = new PagePath(root.getTitle(), root.getId().toString());
        r.setExpanded(true);
        r.setChildren(getChildren(project, root, wikiFolderKey, path.stream().map(p -> p.getId().toString()).collect(Collectors.toList())));
        r.setLeaf(r.getChildren().isEmpty());
        List<PagePath> paths = new ArrayList<>();
        paths.add(r);
        return paths;
    }

    /* Handle Attachments */
    public Set<PageAttachment> getAttachments(String projectKey, Long wikiKey) {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Page not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        return pageAttachmentRepo.findByPageOrderByOriginalNameAsc(wiki).stream().peek(a -> {
            String ext = FilenameUtils.getExtension(a.getOriginalName());
            if (ext.equalsIgnoreCase("png")) a.setPreviewable(true);
            if (ext.equalsIgnoreCase("jpg")) a.setPreviewable(true);
            if (ext.equalsIgnoreCase("jpeg")) a.setPreviewable(true);
            if (ext.equalsIgnoreCase("gif")) a.setPreviewable(true);
            if (ext.equalsIgnoreCase("pdf")) a.setPreviewable(true);
            a.setLocation("/wiki/" + projectKey + "/" + wiki.getId() + "/attachment/" + a.getId() + "/preview/");
        }).collect(Collectors.toSet());
    }

    public ResponseEntity<Resource> getAttachment(String projectKey, Long wikiKey, Long attachmentID) throws
            IOException {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Page not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        PageAttachment a = pageAttachmentRepo.findByPageAndId(wiki, attachmentID);
        if (null == a) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        File file = new File(helperUtil.getDataPath(uploadPath) + a.getName());
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + a.getOriginalName());
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }

    public ResponseEntity<byte[]> getAttachmentPreview(String projectKey, Long wikiKey, Long attachmentID) {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Page not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        PageAttachment a = pageAttachmentRepo.findByPageAndId(wiki, attachmentID);
        if (null == a) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        String ext = FilenameUtils.getExtension(a.getOriginalName());
        if (!ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("gif")
                && !ext.equalsIgnoreCase("pdf")
                && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg")) {
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        byte[] fileContent;
        try {
            fileContent = FileUtils.readFileToByteArray(new File(helperUtil.getDataPath(uploadPath) + a.getName()));
        } catch (IOException e) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        MediaType mediaType = MediaType.IMAGE_JPEG;
        switch (ext) {
            case "png":
                mediaType = MediaType.IMAGE_PNG;
                break;
            case "gif":
                mediaType = MediaType.IMAGE_GIF;
                break;
            case "pdf":
                mediaType = MediaType.APPLICATION_PDF;
                break;
        }
        return ResponseEntity.ok().contentType(mediaType).body(fileContent);
    }

    public PageAttachment saveAttachment(MultipartFile file, String projectKey, Long wikiKey) throws IOException {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (!canEdit(wiki.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        // Get the file and save it somewhere
        byte[] bytes = file.getBytes();
        String name = projectKey + "-W" + wikiKey + "-" + new Date().getTime();
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        PageAttachment exists = pageAttachmentRepo.findByPageAndOriginalName(wiki, file.getOriginalFilename());
        PageAttachment a = new PageAttachment();
        if (null != exists) {
            //Delete old file if exists
            a = exists;
            try {
                File f = new File(helperUtil.getDataPath(uploadPath) + a.getName());
                f.delete();
                if (!a.getThumbnail().isEmpty()) {
                    f = new File(helperUtil.getDataPath(uploadPath) + a.getThumbnail());
                    f.delete();
                }
            } catch (Exception e) {
                //do nothing
            }
        }
        Path path = Paths.get(helperUtil.getDataPath(uploadPath) + name + "." + ext);
        Files.write(path, bytes);
        if (ext.equals("png") | ext.equals("jpg") | ext.equals("jpeg")) {
            File file2 = new File(helperUtil.getDataPath(uploadPath) + name + "." + ext);
            Thumbnails.of(file2).size(100, 100).toFile(new File(helperUtil.getDataPath(uploadPath) + "t_" + name + "." + ext));
            a.setThumbnail("t_" + name + "." + ext);
        }
        a.setPage(wiki);
        a.setName(name + "." + ext);
        a.setOriginalName(file.getOriginalFilename());
        a.setSize(file.getSize());
        a.setType(URLConnection.guessContentTypeFromName(file.getOriginalFilename()));
        pageAttachmentRepo.save(a);
        a.setLocation("/wiki/" + projectKey + "/" + wiki.getId() + "/attachment/" + a.getId() + "/preview/");
        return a;
    }

    public void deleteAttachment(PageAttachment attachment, String projectKey, Long wikiKey) {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (!canEdit(wiki.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        try {
            File file = new File(helperUtil.getDataPath(uploadPath) + attachment.getName());
            file.delete();
            if (!attachment.getThumbnail().isEmpty()) {
                file = new File(helperUtil.getDataPath(uploadPath) + attachment.getThumbnail());
                file.delete();
            }
        } catch (Exception e) {
            //do nothing
        }
        String a = attachment.getOriginalName();
        pageAttachmentRepo.delete(attachment);
    }

    public Set<PageRevision> getRevisions(String projectKey, Long wikiKey) {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Page not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        return pageRevisionRepo.findByPageOrderByIdDesc(wiki);
    }

    @Cacheable("wikiRevision")
    public PageRevision getRevision(String projectKey, Long wikiKey, Long revisionId) {
        Page wiki = getPage(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Page not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        if (revisionId == 0) {
            PageRevision rev = new PageRevision(wiki, 0L, "current", wiki.getLastUpdatedLogin());
            rev.setTitle(wiki.getTitle());
            rev.setContent(wiki.getContent());
            return rev;
        } else {
            return pageRevisionRepo.findByPageAndVersion(wiki, revisionId);
        }
    }

    public void reindexAll() {
        pageCustomRepo.updateAll();
    }

    public List<PagePath> getTree(String projectKey, Long pageKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        if (pageKey == 0) {
            return pageRepo.findByProjectAndParentOrderByTitleAsc(project, null).stream()
                    .map(p -> new PagePath(p.getTitle(), p.getId().toString(), !p.isHasChild()))
                    .sorted(Comparator.comparing(PagePath::getTitle)).collect(Collectors.toUnmodifiableList());
        }
        List<Page> path = getPath(projectKey, pageKey);
        List<PagePath> tree = getTreePath(projectKey, pageKey, path);
        Optional.ofNullable(tree.get(0)).ifPresent(page -> {
            tree.addAll(pageRepo.findByProjectAndParentOrderByTitleAsc(project, null).stream()
                    .filter(p ->
                            !p.getId().equals(Long.parseLong(page.getKey()))
                    ).map(p -> new PagePath(p.getTitle(), p.getId().toString(), !p.isHasChild())).collect(Collectors.toList()));
        });
        return tree.stream().sorted(Comparator.comparing(PagePath::getTitle)).collect(Collectors.toUnmodifiableList());
    }
}
