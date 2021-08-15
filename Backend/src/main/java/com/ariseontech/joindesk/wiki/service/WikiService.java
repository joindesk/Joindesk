package com.ariseontech.joindesk.wiki.service;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.ProjectService;
import com.ariseontech.joindesk.wiki.domain.*;
import com.ariseontech.joindesk.wiki.repo.*;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class WikiService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private AuthService authService;
    @Autowired
    private WikiFolderRepo wikiFolderRepo;
    @Autowired
    private WikiRepo wikiRepo;
    @Autowired
    private WikiCustomRepo wikiCustomRepo;
    @Autowired
    private WikiRevisionRepo wikiRevisionRepo;
    @Autowired
    private Validator validator;
    @Autowired
    private WikiAttachmentRepo wikiAttachmentRepo;
    @Value("${wiki-revision-dir}")
    private String uploadPath;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HelperUtil helperUtil;

    private String decompressGzipFile(String file) throws IOException {
        try (GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(new File(helperUtil.getDataPath(uploadPath) + file)))) {
            return IOUtils.toString(gzipIn);
        }
    }

    private void removeGzipFile(String file) throws IOException {
        FileUtils.forceDelete(new File(helperUtil.getDataPath(uploadPath) + file));
    }

    private void compressGzipFile(String data, String gzipFile) {
        try {
            File file = new File(helperUtil.getDataPath(uploadPath) + gzipFile);
            FileOutputStream fos = new FileOutputStream(file);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            gzipOS.write(data.getBytes());
            gzipOS.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<Wiki> getAllForProject(String projectKey, Wiki parent) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        return wikiRepo.findByProjectOrderByTitleAsc(project);
    }

    public Set<Wiki> getRecentForProject(String projectKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        return wikiRepo.findFirst10ByProjectOrderByUpdatedDesc(project);
    }

    public Wiki getWiki(String projectKey, Long wikiKey, String mode) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        Optional<Wiki> wiki = wikiRepo.findById(wikiKey);
        if (!wiki.isPresent()) throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.get().getProject());
        wiki.get().setEditable(canEdit(project));
        wiki.get().setDeletable(canDelete(project));
        wiki.get().setTree(new ArrayList(Collections.singleton(getWikiSiblings(project, wiki.get()))));
        /*if (mode.equalsIgnoreCase("view")) {
            Set<Login> members = projectService.getMembersByProjectKey(projectKey);
            wiki.get().setContent(replaceInsertsGET(wiki.get(), members));
            Pattern p = Pattern.compile(helperUtil.getUserMentionMatcherRegex());
            Matcher m = p.matcher(wiki.get().getContent());
            while (m.find()) {
                String match = m.group();
                Optional<Login> matched = members.stream().filter(l -> l.getUserName().equalsIgnoreCase(match.substring(1))).findAny();
                if (matched.isPresent()) {
                    wiki.get().setContent(m.replaceFirst("[" + match.substring(1) + "|" + matched.get().getFullName() + "]"));
                } else {
                    wiki.get().setContent(m.replaceFirst("[" + match.substring(1) + "|]"));
                }
                m = p.matcher(wiki.get().getContent());
            }
        }*/
        return wiki.get();
    }

    private WikiPath getWikiSiblings(Project project, Wiki wiki) {
        WikiPath wikiPath = new WikiPath(wiki.getFolder().getTitle(), wiki.getFolder().getId().toString());
        List<WikiPath> siblings = new ArrayList<>();
        wikiRepo.findByProjectAndFolderOrderByTitleAsc(project, wiki.getFolder()).forEach(w -> {
            WikiPath wp = new WikiPath(w.getTitle(), w.getId().toString());
            if (w.getId().equals(wiki.getId()))
                wp.setSelected(true);
            wp.setLeaf(true);
            if (wp.isLeaf())
                wp.setIcon("anticon anticon-file");
            siblings.add(wp);
        });
        wikiPath.setChildren(siblings);
        wikiPath.setExpanded(true);
        return wikiPath;
    }

    public Wiki saveWiki(String projectKey, Wiki wiki) {
        boolean createRevision = false;
        String oldContent = "";
        String oldTitle = "";
        Login currentLogin = authService.currentLogin();
        Login revisionLogin = currentLogin;
        if (wiki.getId() == null || wiki.getId() <= 0) {
            wiki.setCreatedLogin(currentLogin);
        } else {
            Optional<Wiki> w = wikiRepo.findById(wiki.getId());
            if (!w.isPresent())
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            oldContent = w.get().getContent();
            oldTitle = w.get().getTitle();
            wiki.setFolder(w.get().getFolder());
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
        Set<ConstraintViolation<Wiki>> result = validator.validate(wiki);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //If already exists
        if (wiki.getId() != null) {
            Optional<Wiki> webH = wikiRepo.findById(wiki.getId());
            if (webH.isPresent()) {
                wiki.setCreated(webH.get().getCreated());
                wiki.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        wiki.setContent(replaceInsertsPOST(wiki));
        wiki = wikiRepo.save(wiki);
        wikiCustomRepo.update(wiki);
        if (createRevision) {
            Long lastKey = wikiRevisionRepo.findLastVersionForWiki(wiki);
            lastKey = (null == lastKey) ? 0 : lastKey;
            Long rev = lastKey + 1;
            String file = wiki.getProject().getId() + "-" + wiki.getId() + "-" + rev + "-" + new Date().getTime();
            compressGzipFile(oldContent, file);
            WikiRevision wikiRevision = new WikiRevision(wiki, rev, file, revisionLogin);
            wikiRevision.setTitle(oldTitle);
            wikiRevisionRepo.save(wikiRevision);
        }
        return wiki;
    }

    private String replaceInsertsPOST(Wiki wiki) {
        //Replace mentions
        //Set<Login> members = projectService.getMembersByProjectKey(wiki.getProject().getKey());
        /*Matcher m = Pattern.compile(helperUtil.getUserMentionMatcherRegex())
                .matcher(text);
        List<String> unmatched = new ArrayList<>();
        while (m.find() && !unmatched.contains(m.group())) {
            String match = m.group();
            boolean matched = false;
            for (Login l : members) {
                if (l.getUserName().equalsIgnoreCase(match.substring(1))) {
                    matched = true;
                    text = text.replaceAll(match, "<span class='mention' contenteditable='false' data-id='" + l.getId() + "'>" + l.getFullName() + " (" + l.getUserName() + ")</span>");
                    if (issue.getId() != null) {
                        addWatcher(issue, l);
                        issueEventHandler.sendMessage(new IssueEvent(IssueEventType.MENTION, issue, issue.getId(), field, oldText, cleanPreserveLineBreaks(text), currentLogin.getUser()));
                    }
                }
            }
            if (!matched)
                unmatched.add(match);
        }*/
        Document doc = Jsoup.parse(wiki.getContent());
        for (Element mm : doc.select("span.mention")) {
            if (mm.hasAttr("data-new-mention") && mm.hasAttr("data-id")) {
                /*for (Login me : members) {
                    try {
                        if (me.getId().equals(Long.parseLong(mm.attr("data-id")))) {
                            issueEventHandler.sendMessage(new IssueEvent(IssueEventType.MENTION, issue, issue.getId(), field, oldText, cleanPreserveLineBreaks(text), currentLogin.getUser()));
                        }
                    } catch (Exception ignore) {
                    }
                }*/
                mm.removeAttr("data-new-mention");
            }
        }
        for (Element mm : doc.select("img")) {
            if (mm.hasAttr("src") && !mm.hasAttr("onclick")) {
                mm.attr("onclick", "window['previewImageFn'].componentFn(\"" + mm.attr("src") + "\");$event.stopPropagation();");
            }
        }
        for (Element mm : doc.select("a")) {
            if (mm.hasAttr("href") && mm.attr("href").startsWith("jdfile:")) {
                String href = mm.attr("href").substring(mm.attr("href").indexOf(":") + 1);
                mm.attr("onclick", "window['downloadFileFn'].componentFn(" + href.substring(0, href.indexOf(":")) + ",\"" + href.substring(href.indexOf(":") + 1) + "\");$event.stopPropagation();");
            } else if (mm.hasAttr("href")) {
                if (!mm.attr("href").toLowerCase().startsWith(configurationService.getApplicationDomain().toLowerCase()))
                    mm.attr("target", "_blank");
            }
        }
        return doc.toString();
    }

    private String replaceInsertsGET(Wiki wiki, Set<Login> members) {
        Document doc = Jsoup.parse(wiki.getContent());
        for (Element mm : doc.select("span.mention")) {
            for (Login m : members) {
                try {
                    if (m.getId().equals(Long.parseLong(mm.attr("data-id")))) {
                        mm.html(m.getFullName());
                    }
                } catch (Exception ignore) {
                }
            }
        }
        return doc.toString();
    }

    public void removeWiki(Long wikiKey) {
        Optional<Wiki> wiki = wikiRepo.findById(wikiKey);
        if (!wiki.isPresent()) throw new JDException("", ErrorCode.ISSUE_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.get().getProject());
        if (!authService.hasAuthorityForProject(wiki.get().getProject(), AuthorityCode.WIKI_DELETE)) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        removeWiki(wiki.get());
    }

    private void removeWiki(Wiki wiki) {
        //Delete versions
        wikiRevisionRepo.findByWikiOrderByIdDesc(wiki).forEach(wr -> {
            try {
                removeGzipFile(wr.getFilename());
            } catch (IOException e) {
                //ignore
            }
            wikiRevisionRepo.delete(wr);
        });
        wikiAttachmentRepo.findByWiki(wiki).forEach(wikiAttachment ->
                deleteAttachment(wikiAttachment, wikiAttachment.getWiki().getProject().getKey(), wikiAttachment.getWiki().getId()));
        wikiRepo.delete(wiki);
    }

    public void removeWikiFolder(Long wikiFolderKey) {
        Optional<WikiFolder> wikiFolder = wikiFolderRepo.findById(wikiFolderKey);
        if (!wikiFolder.isPresent()) throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wikiFolder.get().getProject());
        if (!authService.hasAuthorityForProject(wikiFolder.get().getProject(), AuthorityCode.WIKI_DELETE) || wikiFolder.get().isProjectDefault()) {
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        removeWikiFolder(wikiFolder.get());
        //Delete actual folder
        wikiFolderRepo.delete(wikiFolder.get());
    }

    private void removeWikiFolder(WikiFolder wikiFolder) {
        Set<WikiFolder> folders = wikiFolderRepo.findByProjectAndParentOrderByTitleAsc(wikiFolder.getProject(), wikiFolder);
        if (!folders.isEmpty()) {
            //Recurse
            folders.forEach(this::removeWikiFolder);
        } else {
            //Delete folder containing wikis
            wikiRepo.findByProjectAndFolderOrderByTitleAsc(wikiFolder.getProject(), wikiFolder).forEach(this::removeWiki);
            //delete folder itself
            updateParent(wikiFolder.getParent().getId());
            wikiFolderRepo.delete(wikiFolder);
        }
    }

    private void updateParent(Long parentFolderID) {
        Optional<WikiFolder> parentFolder = wikiFolderRepo.findById(parentFolderID);
        parentFolder.ifPresent(this::updateParent);
    }

    private void updateParent(WikiFolder pF) {
        pF.setHasChildFolders(wikiFolderRepo.countByProjectAndParent(pF.getProject(), pF) > 0);
        wikiFolderRepo.save(pF);
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

    public List<Wiki> searchWiki(String projectKey, String q) {
        //q = q + "*";
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        return wikiCustomRepo.filter(q.replaceAll(" ", "|"), project);
        //return wikiLuceneService.searchWiki(q.toLowerCase() + " AND project:" + projectKey);
    }

    public void copyWiki(String projectKey, Long wikiKey, WikiFolder wikiF) {
        Project project = projectService.findByKey(projectKey);
        if (!canEdit(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<Wiki> wiki = wikiRepo.findById(wikiKey);
        if (!wiki.isPresent()) throw new JDException("Document not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Optional<WikiFolder> wikiFolder = wikiFolderRepo.findById(wikiF.getId());
        if (!wikiFolder.isPresent())
            throw new JDException("Folder not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        wiki.ifPresent(w -> {
            Wiki wikiCopy = new Wiki();
            wikiCopy.setFolder(wikiFolder.get());
            wikiCopy.setTitle("Copy of " + wiki.get().getTitle());
            wikiCopy.setContent(wiki.get().getContent());
            wikiCopy.setProject(wiki.get().getProject());
            saveWiki(projectKey, wikiCopy);
        });
    }

    public void moveWiki(String projectKey, Long wikiKey, WikiFolder wikiF) {
        Project project = projectService.findByKey(projectKey);
        if (!canEdit(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<Wiki> wiki = wikiRepo.findById(wikiKey);
        if (!wiki.isPresent()) throw new JDException("Document not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        Optional<WikiFolder> wikiFolder = wikiFolderRepo.findById(wikiF.getId());
        if (!wikiFolder.isPresent())
            throw new JDException("Folder not found", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        wiki.ifPresent(w -> {
            w.setFolder(wikiFolder.get());
            wikiRepo.save(w);
        });
    }

    public void moveWikiFolder(String projectKey, Long folderKey, WikiFolder wikiFolder) {
        Project project = projectService.findByKey(projectKey);
        if (!canEdit(project))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<WikiFolder> folderToMove = wikiFolderRepo.findById(folderKey);
        Optional<WikiFolder> destFolder = wikiFolderRepo.findById(wikiFolder.getId());
        if (!folderToMove.isPresent() || !destFolder.isPresent() || destFolder.get().getId().equals(folderToMove.get().getId())) {
            throw new JDException("Invalid Move Operation", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        } else {
            WikiFolder sourceFolder = folderToMove.get();
            sourceFolder.setParent(destFolder.get());
            wikiFolderRepo.save(sourceFolder);
            updateParent(sourceFolder);
            updateParent(destFolder.get());
        }
    }

    public Set<WikiFolder> searchFolder(String projectKey, String q) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        return wikiFolderRepo.findTop10ByProjectAndTitleIgnoreCaseContainingOrderByTitleAsc(project, q);
    }

    @Cacheable(value = "projectDefaultWikiFolder", key = "#p.id")
    public WikiFolder getProjectDefault(Project p) {
        WikiFolder d = wikiFolderRepo.findByProjectAndProjectDefaultTrue(p);
        if (d == null) {
            WikiFolder w = new WikiFolder();
            w.setTitle(p.getName() + " Home");
            w.setProject(p);
            w.setProjectDefault(true);
            w.setCreatedLogin(authService.currentLogin());
            d = wikiFolderRepo.save(w);
            Wiki wiki = new Wiki();
            wiki.setTitle("Getting started");
            wiki.setContent("Welcome to Wiki");
            wiki.setProject(p);
            wiki.setCreatedLogin(authService.currentLogin());
            wiki.setFolder(d);
            wikiRepo.save(wiki);
        }
        return d;
    }

    public WikiData getFolder(String projectKey, Long wikiFolderKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        WikiData data = new WikiData();
        Optional<WikiFolder> parentFolder = wikiFolderRepo.findById(wikiFolderKey);
        parentFolder.ifPresent(wikiFolder -> {
            wikiFolder.setEditable(canEdit(project));
            wikiFolder.setDeletable(wikiFolder.getParent() != null && canDelete(project));
            data.setWikiFolder(wikiFolder);
            data.setWikiFolders(wikiFolderRepo.findByProjectAndParentOrderByTitleAsc(project, wikiFolder));
            data.setWikis(wikiRepo.findByProjectAndFolderOrderByTitleAsc(project, wikiFolder));
            data.setPath(getPath(projectKey, wikiFolderKey));
            data.setTree(getTreePath(projectKey, wikiFolderKey, data.getPath()));
        });
        return data;
    }

    public List<WikiPath> getFolderChildrens(String projectKey, Long wikiFolderKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        Optional<WikiFolder> parentFolder = wikiFolderRepo.findById(wikiFolderKey);
        return parentFolder.map(wikiFolder -> getChildren(project, wikiFolder, 0L, new ArrayList<>())).orElseGet(ArrayList::new);
    }

    private List<WikiFolder> getPath(String projectKey, Long wikiFolderKey) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        List<WikiFolder> folders = new ArrayList<>();
        Optional<WikiFolder> folder = wikiFolderRepo.findById(wikiFolderKey);
        if (folder.isPresent() && folder.get().getParent() != null) {
            do {
                folder = wikiFolderRepo.findById(folder.get().getParent().getId());
                folders.add(folder.get());
            }
            while (folder.get().getParent() != null);
        }
        return folders;
    }

    private List<WikiPath> getChildren(Project project, WikiFolder wikiFolder, Long selectFolderKey, List<String> pathsKeys) {
        List<WikiPath> childrens = new ArrayList<>();
        wikiFolderRepo.findByProjectAndParentOrderByTitleAsc(project, wikiFolder).forEach(wf -> {
            WikiPath wp = new WikiPath(wf.getTitle(), wf.getId().toString());
            wp.setFolder(wf);
            if (pathsKeys.contains(wp.getKey()))
                wp.setExpanded(true);
            if (wf.getId().equals(selectFolderKey))
                wp.setSelected(true);
            wp.setLeaf(!wf.isHasChildFolders());
            if (wp.isLeaf())
                wp.setIcon("anticon anticon-file");
            if (wp.isExpanded() || wp.isSelected())
                wp.setChildren(getChildren(project, wf, selectFolderKey, pathsKeys));
            childrens.add(wp);
        });
        return childrens;
    }

    public List<WikiPath> getTreePath(String projectKey, Long wikiFolderKey, List<WikiFolder> path) {
        Project project = projectService.findByKey(projectKey);
        hasView(project);
        WikiFolder root;
        if (path.isEmpty()) {
            root = wikiFolderRepo.findById(wikiFolderKey).get();
        } else {
            root = (WikiFolder) new ReverseListIterator(path).next();
        }
        WikiPath r = new WikiPath(root.getTitle(), root.getId().toString());
        r.setExpanded(true);
        r.setChildren(getChildren(project, root, wikiFolderKey, path.stream().map(p -> p.getId().toString()).collect(Collectors.toList())));
        List<WikiPath> paths = new ArrayList<>();
        paths.add(r);
        return paths;
    }

    public WikiFolder saveWikiFolder(String projectKey, Long wikiFolderKey, WikiFolder wikiFolder) {
        Optional<WikiFolder> parentFolder = wikiFolderRepo.findById(wikiFolderKey);
        if (!parentFolder.isPresent() || !parentFolder.get().getProject().getKey().equalsIgnoreCase(projectKey)) {
            throw new JDException("Invalid parent", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        } else {
            wikiFolder.setParent(parentFolder.get());
            wikiFolder.setProject(projectService.findByKey(projectKey));
            checkWikiFolder(wikiFolder);
            hasView(wikiFolder.getProject());
            if (!canEdit(wikiFolder.getProject()))
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            validateWikiFolder(wikiFolder);
            wikiFolder = wikiFolderRepo.save(wikiFolder);
            //update parent
            updateParent(parentFolder.get());
            wikiFolderRepo.save(parentFolder.get());
            return wikiFolder;
        }
    }

    public void renameWikiFolder(String projectKey, WikiFolder wikiFolder) {
        Optional<WikiFolder> folder = wikiFolderRepo.findById(wikiFolder.getId());
        folder.ifPresent(f -> {
            hasView(f.getProject());
            if (!canEdit(f.getProject()) || f.isProjectDefault())
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            f.setTitle(wikiFolder.getTitle());
            checkWikiFolder(f);
            validateWikiFolder(f);
            wikiFolderRepo.save(f);
        });
    }

    private void validateWikiFolder(WikiFolder wikiFolder) {
        Set<ConstraintViolation<WikiFolder>> result = validator.validate(wikiFolder);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
    }

    private void checkWikiFolder(WikiFolder wikiFolder) {
        wikiFolderRepo.findByProjectAndParentOrderByTitleAsc(wikiFolder.getParent().getProject(), wikiFolder.getParent())
                .stream().filter(f -> {
            if (wikiFolder.getId() == null)
                return f.getTitle().equalsIgnoreCase(wikiFolder.getTitle());
            else return !f.getId().equals(wikiFolder.getId()) && f.getTitle().equalsIgnoreCase(wikiFolder.getTitle());
        }).findAny().ifPresent(m -> {
            throw new JDException("Folder already exists", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        });
        if (wikiFolder.getId() == null || wikiFolder.getId() <= 0) {
            wikiFolder.setCreatedAt(new Timestamp(new Date().getTime()));
            wikiFolder.setCreatedLogin(authService.currentLogin());
        } else {
            wikiFolder.setLastUpdatedLogin(authService.currentLogin());
            wikiFolder.setUpdatedAt(new Timestamp(new Date().getTime()));
        }
    }

    /* Handle Attachments */
    public Set<WikiAttachment> getAttachments(String projectKey, Long wikiKey) {
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Wiki not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        return wikiAttachmentRepo.findByWikiOrderByOriginalNameAsc(wiki).stream().peek(a -> {
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
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Wiki not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        WikiAttachment a = wikiAttachmentRepo.findByWikiAndId(wiki, attachmentID);
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
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Wiki not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        WikiAttachment a = wikiAttachmentRepo.findByWikiAndId(wiki, attachmentID);
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

    public WikiAttachment saveAttachment(MultipartFile file, String projectKey, Long wikiKey) throws IOException {
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (!canEdit(wiki.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        // Get the file and save it somewhere
        byte[] bytes = file.getBytes();
        String name = projectKey + "-W" + wikiKey + "-" + new Date().getTime();
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        WikiAttachment exists = wikiAttachmentRepo.findByWikiAndOriginalName(wiki, file.getOriginalFilename());
        WikiAttachment a = new WikiAttachment();
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
        a.setWiki(wiki);
        a.setName(name + "." + ext);
        a.setOriginalName(file.getOriginalFilename());
        a.setSize(file.getSize());
        a.setType(URLConnection.guessContentTypeFromName(file.getOriginalFilename()));
        wikiAttachmentRepo.save(a);
        a.setLocation("/wiki/" + projectKey + "/" + wiki.getId() + "/attachment/" + a.getId() + "/preview/");
        return a;
    }

    public void deleteAttachment(WikiAttachment attachment, String projectKey, Long wikiKey) {
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
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
        wikiAttachmentRepo.delete(attachment);
    }


    public Set<WikiRevision> getRevisions(String projectKey, Long wikiKey) {
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Wiki not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        return wikiRevisionRepo.findByWikiOrderByIdDesc(wiki);
    }

    @Cacheable("wikiRevision")
    public WikiRevision getRevision(String projectKey, Long wikiKey, Long revisionId) {
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Wiki not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        if (revisionId == 0) {
            WikiRevision rev = new WikiRevision(wiki, 0L, "current", wiki.getLastUpdatedLogin());
            rev.setTitle(wiki.getTitle());
            rev.setContent(wiki.getContent());
            return rev;
        } else {
            WikiRevision rev = wikiRevisionRepo.findByWikiAndVersion(wiki, revisionId);
            try {
                rev.setContent(decompressGzipFile(rev.getFilename()));
            } catch (IOException e) {
                throw new JDException("Error getting revision", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
            }
            return rev;
        }
    }

    public String compareRevision(String projectKey, Long wikiKey, Long sourceRev, Long targetRev) {
        Wiki wiki = getWiki(projectKey, wikiKey, "view");
        if (null == wiki) throw new JDException("Wiki not found", ErrorCode.ISSUE_NOT_FOUND, HttpStatus.NOT_FOUND);
        hasView(wiki.getProject());
        try {
            String source, target = null;
            if (sourceRev == 0)
                source = wiki.getContent();
            else
                source = decompressGzipFile(wikiRevisionRepo.findByWikiAndVersion(wiki, sourceRev).getFilename());
            if (targetRev == 0)
                target = wiki.getContent();
            else {
                target = decompressGzipFile(wikiRevisionRepo.findByWikiAndVersion(wiki, targetRev).getFilename());
            }
            return new JSONObject().put("source", source).put("target", target).toString();
        } catch (IOException e) {
            throw new JDException("Error comparing selected version", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
        }
    }

    public void reindexAll() {
        wikiCustomRepo.updateAll();
    }
}
