package com.ariseontech.joindesk.git.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.git.domain.GitHook;
import com.ariseontech.joindesk.git.domain.Repository;
import com.ariseontech.joindesk.git.service.GitRepositoryService;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/project/{project_key}/git/repo", produces = "application/json", consumes = "application/json")
public class GitRepositoryController {

    @Autowired
    private GitRepositoryService gitRepositoryService;

    @Autowired
    private ProjectService projectService;

    @GetMapping("")
    public String getAll(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("-user_detail", gitRepositoryService.getAllRepository(projectService.findByKey(projectKey)));
    }

    @GetMapping("/{repo_id}/hook")
    public String getAllHooks(@PathVariable("repo_id") Long repoId) {
        return HelperUtil.squiggly("-user_detail", gitRepositoryService.getAllHooks(repoId));
    }

    @GetMapping("/{repo_id}/hook/{id}")
    public String getHook(@PathVariable("repo_id") Long repoId, @PathVariable("id") Long id) {
        return HelperUtil.squiggly("-user_detail", gitRepositoryService.getHook(repoId, id));
    }

    @RequestMapping(method = RequestMethod.POST, value = "save")
    public String save(@PathVariable("project_key") String projectKey, @RequestBody Repository repository) {
        repository.setProject(projectService.findByKey(projectKey));
        return HelperUtil.squiggly("base,-user_detail", gitRepositoryService.saveRepo(repository));
    }

    @RequestMapping(method = RequestMethod.POST, value = "{repo_id}/hook/save")
    public String saveHook(@PathVariable("repo_id") Long repoId, @RequestBody GitHook hook) {
        return HelperUtil.squiggly("base,-user_detail", gitRepositoryService.saveHook(repoId, hook));
    }

    @RequestMapping(method = RequestMethod.POST, value = "{repo_id}/hook/delete")
    public void remove(@PathVariable("project_key") String projectKey, @PathVariable("repo_id") Long repoId, @RequestBody GitHook gitHook) {
        gitRepositoryService.deleteHook(repoId, gitHook);
    }

    /* For Issue view */
    @GetMapping("/branches")
    public String getBranches(@PathVariable("project_key") String projectKey, @RequestParam("id") Long issueId) {
        return HelperUtil.squiggly("-user_detail", gitRepositoryService.getBranches(projectKey,issueId));
    }

    @GetMapping("/commits")
    public String getCommits(@PathVariable("project_key") String projectKey, @RequestParam("id") Long issueId) {
        return HelperUtil.squiggly("-user_detail", gitRepositoryService.getCommits(projectKey,issueId));
    }
}
