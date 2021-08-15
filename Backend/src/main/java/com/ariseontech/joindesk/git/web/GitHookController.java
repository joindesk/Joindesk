package com.ariseontech.joindesk.git.web;

import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.git.service.GitHookService;
import com.ariseontech.joindesk.git.service.GitRepositoryService;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/hook/git/", produces = "application/json", consumes = "application/json")
public class GitHookController {

    @Autowired
    private GitRepositoryService gitRepositoryService;
    @Autowired
    private GitHookService gitHookService;
    @Autowired
    private ProjectService projectService;

    @PostMapping("{uuid}")
    public void receiveHooks(@PathVariable("uuid") String repoId, @RequestBody String payload,
                             HttpServletRequest request) {
        gitHookService.receiveHook(repoId, payload, request);
    }

}
