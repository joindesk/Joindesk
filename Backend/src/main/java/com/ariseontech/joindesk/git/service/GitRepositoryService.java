package com.ariseontech.joindesk.git.service;

import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.git.domain.GitBranch;
import com.ariseontech.joindesk.git.domain.GitCommit;
import com.ariseontech.joindesk.git.domain.GitHook;
import com.ariseontech.joindesk.git.domain.Repository;
import com.ariseontech.joindesk.git.repo.GitBranchRepo;
import com.ariseontech.joindesk.git.repo.GitCommitRepo;
import com.ariseontech.joindesk.git.repo.HookRepo;
import com.ariseontech.joindesk.git.repo.RepositoryRepo;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GitRepositoryService {

    @Autowired
    private RepositoryRepo repositoryRepo;
    @Autowired
    private HookRepo hookRepo;
    @Autowired
    private GitCommitRepo gitCommitRepo;
    @Autowired
    private GitBranchRepo gitBranchRepo;
    @Autowired
    private IssueService issueService;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ConfigurationService configurationService;

    public Set<GitHook> getAllHooks(Long repoId) {
        Optional<Repository> r = getRepo(repoId);
        if (r.isEmpty())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        checkManageAccess(r.get().getProject());
        return hookRepo.findAllByRepository(r.get());
    }

    public Set<GitHook> getAllHooks(Repository repository) {
        checkManageAccess(repository.getProject());
        return hookRepo.findAllByRepository(repository);
    }

    public Set<Repository> getAllRepository(Project project) {
        checkManageAccess(project);
        return repositoryRepo.findAllByProject(project).stream().peek(rr -> rr.setHookEndpoint(configurationService.getApplicationDomain() + "git/hook/" + rr.getUuid())).collect(Collectors.toSet());
    }

    public Optional<Repository> getRepo(Long id) {
        Optional<Repository> repo = repositoryRepo.findById(id);
        repo.ifPresent(repository -> checkManageAccess(repository.getProject()));
        return repo;
    }

    public Optional<GitHook> getHook(Long repoId, Long id) {
        Optional<GitHook> hook = hookRepo.findById(id);
        hook.ifPresent(h -> checkManageAccess(h.getRepository().getProject()));
        if (hook.isPresent() && !hook.get().getRepository().getId().equals(repoId))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return hook;
    }

    public Repository saveRepo(Repository repository) {
        checkManageAccess(repository.getProject());
        //If already exists
        if (repository.getId() != null) {
            Optional<Repository> webH = repositoryRepo.findById(repository.getId());
            if (webH.isPresent()) {
                repository.setCreated(webH.get().getCreated());
                repository.setCreatedBy(webH.get().getCreatedBy());
            }
        } else
            repository.setUuid(UUID.randomUUID() + "0" + repository.getName().hashCode());
        repository = repositoryRepo.save(repository);
        return repository;
    }

    public GitHook saveHook(Long repoId, GitHook gitHook) {
        Optional<Repository> r = getRepo(repoId);
        if (r.isEmpty())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        gitHook.setRepository(r.get());
        checkManageAccess(gitHook.getRepository().getProject());
        //If already exists
        if (gitHook.getId() != null) {
            Optional<GitHook> webH = hookRepo.findById(gitHook.getId());
            if (webH.isPresent()) {
                gitHook.setCreated(webH.get().getCreated());
                gitHook.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        gitHook = hookRepo.save(gitHook);
        return gitHook;
    }

    public void deleteHook(Long repoId, GitHook gitHook) {
        hookRepo.findById(gitHook.getId()).ifPresent(g -> {
            checkManageAccess(g.getRepository().getProject());
            if (!g.getRepository().getId().equals(repoId))
                throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            hookRepo.delete(g);
        });
    }

    public boolean hasManageAccess(Project project) {
        return projectService.hasProjectManageAccess(project);
    }

    private void checkManageAccess(Project project) {
        if (!hasManageAccess(project)) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    public boolean hasProjectViewAccess(Project project) {
        return projectService.hasProjectViewAccess(project);
    }

    private void checkViewAccess(Project project) {
        if (!hasProjectViewAccess(project)) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    public Set<GitBranch> getBranches(String projectKey, Long issueId) {
        String issueKey = projectKey + "-" + issueId;
        Project project = projectService.findByKey(issueService.getProjectKeyFromPair(issueKey));
        if (null != project) {
            return gitBranchRepo.findByIssuesContainingIgnoreCase(issueKey).stream().peek(b -> b.setRepoName(b.getRepository().getName())).collect(Collectors.toSet());
        }
        return null;
    }

    public Set<GitCommit> getCommits(String projectKey, Long issueId) {
        String issueKey = projectKey + "-" + issueId;
        Project project = projectService.findByKey(issueService.getProjectKeyFromPair(issueKey));
        if (null != project) {
            return gitCommitRepo.findByIssuesContainingIgnoreCase(issueKey).stream().peek(b -> b.setRepoName(b.getRepository().getName())).collect(Collectors.toSet());
        }
        return null;
    }
}
