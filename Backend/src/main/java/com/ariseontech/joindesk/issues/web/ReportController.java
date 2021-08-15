package com.ariseontech.joindesk.issues.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.repo.ReportDTO;
import com.ariseontech.joindesk.issues.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping(produces = "application/json", value = SystemInfo.apiPrefix + "/report/{project_key}")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public String getReport(@PathVariable("project_key") String projectKey) {
        return HelperUtil.squiggly("base", reportService.getReport(projectKey));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/")
    public String getReportData(@PathVariable("project_key") String projectKey, @RequestBody ReportDTO reportDTO) throws ParseException {
        return HelperUtil.squiggly("base", reportService.getReportData(projectKey, reportDTO));
    }
}
