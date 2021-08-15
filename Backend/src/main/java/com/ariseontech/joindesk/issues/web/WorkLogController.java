package com.ariseontech.joindesk.issues.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.domain.WorkLog;
import com.ariseontech.joindesk.issues.service.WorkLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/worklog/{projectKey}/", produces = "application/json")
public class WorkLogController {

    @Autowired
    private WorkLogService workLogService;
    
    @RequestMapping(value = "{issueID}", method = RequestMethod.GET)
    public String getWorkLogsForIssue(@PathVariable("projectKey") String projectKey, @PathVariable("issueID") Long issueKey) {
        return HelperUtil.squiggly("base,audit_details", workLogService.getAllForProject(projectKey, issueKey).stream().sorted(Comparator.comparingLong(w -> w.getWorkFrom().getTime())).collect(Collectors.toSet()));
    }

    @RequestMapping(value = "{issueID}/log/{workLogID}", method = RequestMethod.GET)
    public String getWorkLogForIssue(@PathVariable("projectKey") String projectKey, @PathVariable("issueID") Long issueKey
            , @PathVariable("workLogID") Long workLogID) {
        return HelperUtil.squiggly("base,audit_details", workLogService.getWorkLog(projectKey, issueKey, workLogID));
    }

    @RequestMapping(value = "{issueID}/log", method = RequestMethod.POST)
    public String saveWorkLog(@RequestBody WorkLog workLog, @PathVariable("projectKey") String projectKey, @PathVariable("issueID") Long issueKey) {
        return HelperUtil.squiggly("base", workLogService.save(projectKey, issueKey, workLog));
    }

    @RequestMapping(value = "{issueID}/log/{workLogID}", method = RequestMethod.DELETE)
    public String deleteWorkLog(@PathVariable("projectKey") String projectKey, @PathVariable("issueID") Long issueKey
            , @PathVariable("workLogID") Long workLogID) {
        workLogService.delete(projectKey, issueKey, workLogID);
        return HelperUtil.squiggly("base", "");
    }
}
