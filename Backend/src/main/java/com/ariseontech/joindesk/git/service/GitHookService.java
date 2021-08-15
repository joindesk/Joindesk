package com.ariseontech.joindesk.git.service;

import com.ariseontech.joindesk.git.domain.GitBranch;
import com.ariseontech.joindesk.git.domain.GitCommit;
import com.ariseontech.joindesk.git.domain.GitRepoType;
import com.ariseontech.joindesk.git.domain.Repository;
import com.ariseontech.joindesk.git.repo.GitBranchRepo;
import com.ariseontech.joindesk.git.repo.GitCommitRepo;
import com.ariseontech.joindesk.git.repo.RepositoryRepo;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
public class GitHookService {

    final String issueKeyMatcher = "((?<!([A-Za-z]{1,10})-?)[A-Z]+-\\d+)";
    final Pattern issueKeyPattern = Pattern.compile(issueKeyMatcher);
    @Autowired
    private RepositoryRepo repositoryRepo;
    @Autowired
    private IssueService issueService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private GitCommitRepo gitCommitRepo;
    @Autowired
    private GitBranchRepo gitBranchRepo;

    public void receiveHook(String uuid, String payload, HttpServletRequest request) {
        Optional.ofNullable(repositoryRepo.findByUuid(uuid)).ifPresent(r -> {
            JSONObject data = new JSONObject(payload);
            if (r.getRepoType().equals(GitRepoType.GITHUB)) {
                if (!data.has("repository")) return;
                JSONObject repository = data.getJSONObject("repository");
                if (!r.getRepoUrl().equalsIgnoreCase(repository.getString("html_url"))
                        && !r.getRepoUrl().equalsIgnoreCase(repository.getString("ssh_url"))
                        && !r.getRepoUrl().equalsIgnoreCase(repository.getString("git_url")))
                    return;
                System.out.println(request.getHeader("X-GitHub-Event"));
                String event = request.getHeader("X-GitHub-Event");
                handleGithubEvent(event, data, r);
            }
            System.out.println(data.toString(2));
        });

    }

    private void handleGithubEvent(String event, JSONObject payload, Repository repository) {
        switch (event) {
            case "create":
                if (payload.has("ref_type")
                        && payload.has("created") && payload.getBoolean("created")
                        && payload.getString("ref_type").equalsIgnoreCase("branch")) {
                    GitBranch branch = new GitBranch(payload.getString("ref"), "");
                    if (null != gitBranchRepo.findByNameAndRepository(branch.getName(), repository))
                        return;
                    StringBuilder sb = new StringBuilder();
                    issueKeyPattern
                            .matcher(branch.getName())
                            .results()
                            .map(MatchResult::group)
                            .forEach(s -> {
                                if (sb.length() > 0) sb.append(",");
                                sb.append(s);
                            });
                    branch.setIssues(sb.toString());
                    branch.setRepository(repository);
                    branch.setUrl(payload.getString("compare"));
                    gitBranchRepo.save(branch);
                }
                break;
            case "delete":
                if (payload.has("ref_type") && payload.getString("ref_type").equalsIgnoreCase("branch")) {
                    Optional.ofNullable(gitBranchRepo.findByName(payload.getString("ref")))
                            .ifPresent(b -> gitBranchRepo.delete(b));
                }
                break;
            case "push":
                if (payload.has("commits")) {
                    JSONArray commits = payload.getJSONArray("commits");
                    for (int i = 0; i < commits.length(); i++) {
                        JSONObject c = commits.getJSONObject(i);
                        GitCommit commit = new GitCommit();
                        commit.setCommitId(c.getString("id"));
                        if (null != gitCommitRepo.findByCommitIdAndRepository(commit.getCommitId(), repository))
                            return;
                        commit.setName(c.getString("message"));
                        StringBuilder sb = new StringBuilder();
                        issueKeyPattern
                                .matcher(commit.getName())
                                .results()
                                .map(MatchResult::group)
                                .forEach(s -> {
                                    if (sb.length() > 0) sb.append(",");
                                    sb.append(s);
                                });
                        commit.setIssues(sb.toString());
                        commit.setRepository(repository);
                        if (c.has("author"))
                            commit.setAuthor(c.getJSONObject("author").getString("name"));
                        commit.setAdded(c.getJSONArray("added").length());
                        commit.setRemoved(c.getJSONArray("removed").length());
                        commit.setModified(c.getJSONArray("modified").length());
                        commit.setUrl(c.getString("url"));
                        //commit.setTimestamp(LocalDate.parse(c.getString("timestamp")));
                        gitCommitRepo.save(commit);
                    }
                } else if (payload.has("pull_request")) {
                    //JSONArray pullrequests = payload.getJSONArray("pull_request");
                }
                break;
        }
    }

}
