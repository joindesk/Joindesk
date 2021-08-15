package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.*;
import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.domain.TimeTracking;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

@Service
public class ReportService {
    SimpleDateFormat reportDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    @Autowired
    private IssueRepo issueRepo;
    @Autowired
    private WorkflowRepo workflowRepo;
    @Autowired
    private WorkflowStepRepo workflowStepRepo;
    @Autowired
    private IssueService issueService;
    @Autowired
    private LoginRepo loginRepo;
    @Autowired
    private IssueTypeRepo issueTypeRepo;
    @Autowired
    private ResolutionRepo resolutionRepo;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WorkLogService workLogService;
    @Autowired
    private CurrentLogin currentLogin;

    public ReportDTO getReport(String projectKey) {
        ReportDTO report = new ReportDTO();
        report.setReportZone(currentLogin.getUser().getTimezone().getID());
        report.setReportFrom(LocalDate.now().minusDays(30));
        report.setReportTo(LocalDate.now());
        report.setType("rcir");
        report.setGroup("daily");
        report.setPcrGroup("assignee");
        return report;
    }

    public ReportDTO getReportData(String projectKey, ReportDTO reportDTO) {
        Project p = projectService.findByKey(projectKey);
        if (null == p) throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!projectService.hasProjectViewAccess(p) || reportDTO.getReportFrom() == null || reportDTO.getReportTo() == null)
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        ZoneId zone = ZoneId.of(reportDTO.getReportZone());
        ZonedDateTime reportFrom = LocalDateTime.of(reportDTO.getReportFrom(), LocalTime.of(0, 0, 0)).atZone(zone);
        ZonedDateTime reportTo = LocalDateTime.of(reportDTO.getReportTo(), LocalTime.of(23, 0, 0)).atZone(zone);
        reportDTO.setReportFrom(reportFrom.toLocalDate());
        reportDTO.setReportTo(reportTo.toLocalDate());
        ZoneId zoneUTC = ZoneId.of("UTC");
        reportFrom = reportFrom.withZoneSameInstant(zoneUTC);
        reportTo = reportTo.withZoneSameInstant(zoneUTC);
        String from = reportFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String to = reportTo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String range = "(created between '" + from + "' AND '" + to + "')";
        JSONArray labels = new JSONArray();
        IssueSearchDTO issueSearchDTO = new IssueSearchDTO();
        issueSearchDTO.setPageIndex(1);
        issueSearchDTO.setPageSize(100000000);
        issueSearchDTO.setFilter(reportDTO.getFilter());
        issueSearchDTO.setProjectKey(projectKey);
        issueSearchDTO.setTimezone(currentLogin.getUser().getTimezone().getID());
        int max = 2;
        String[] colors = {
                "#39add1", // light blue
                "#3079ab", // dark blue
                "#c25975", // mauve
                "#e15258", // red
                "#f9845b", // orange
                "#838cc7", // lavender
                "#7d669e", // purple
                "#53bbb4", // aqua
                "#51b46d", // green
                "#e0ab18", // mustard
                "#637a91", // dark gray
                "#f092b0", // pink
                "#b7c0c7"  // light gray
        };
        switch (reportDTO.getType()) {
            case "crir":
                //if (!reportDTO.getQ().isEmpty()) range = range + " AND " + reportDTO.getQ();
                System.out.println(range);
                IssueSearchDTO issues = issueService.searchIssuesLucene(projectKey, issueSearchDTO, range);
                Map<String, Integer> created = new TreeMap<>();
                Map<String, Integer> resolved = new TreeMap<>();
                Map<Integer, LocalDate> weekMap = new TreeMap<>();
                JSONArray createdArray = new JSONArray();
                JSONArray resolvedArray = new JSONArray();
                for (LocalDate date = reportFrom.toLocalDate(); date.isBefore(reportDTO.getReportTo().plusDays(1)); date = date.plusDays(1)) {
                    switch (reportDTO.getGroup().toLowerCase()) {
                        case "weekly":
                            int weekOfYear = date.get(WeekFields.of(Locale.US).weekOfWeekBasedYear());
                            if (!weekMap.containsKey(weekOfYear) || weekMap.get(weekOfYear).isAfter(date)) {
                                weekMap.put(weekOfYear, date);
                            }
                            break;
                        case "monthly":
                            created.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-01")), 0);
                            resolved.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-01")), 0);
                            break;
                        default:
                            created.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                            resolved.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                            break;
                    }
                }
                //For weekly
                if (reportDTO.getGroup().equalsIgnoreCase("weekly")) {
                    weekMap.forEach((k, v) -> {
                        created.put(v.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                        resolved.put(v.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                    });
                }
                for (Issue issue : issues.getIssues()) {
                    String createdDate = reportDateFormat.format(issue.getCreated());
                    String resolvedDate = issue.getResolvedDate() != null ? issue.getResolvedDate().toString() : "";
                    if (reportDTO.getGroup().equalsIgnoreCase("monthly")) {
                        createdDate = new SimpleDateFormat("yyyy-MM-01").format(issue.getCreated());
                        resolvedDate = issue.getResolvedDate() != null ? new SimpleDateFormat("yyyy-MM-01").format(issue.getResolvedDate()) : "";
                    } else if (reportDTO.getGroup().equalsIgnoreCase("weekly")) {
                        LocalDate c = LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(issue.getCreated()));
                        int weekOfYear = c.get(WeekFields.of(Locale.US).weekOfWeekBasedYear());
                        createdDate = weekMap.containsKey(weekOfYear) ? weekMap.get(weekOfYear).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
                        if (issue.getResolvedDate() != null) {
                            weekOfYear = issue.getResolvedDate().get(WeekFields.of(Locale.US).weekOfWeekBasedYear());
                            resolvedDate = weekMap.containsKey(weekOfYear) ? weekMap.get(weekOfYear).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
                        }
                    }
                    if (created.containsKey(createdDate)) created.put(createdDate, created.get(createdDate) + 1);
                    if (!ObjectUtils.isEmpty(resolvedDate) && resolved.containsKey(resolvedDate))
                        resolved.put(resolvedDate, resolved.get(resolvedDate) + 1);
                }
                created.forEach((key, value) -> {
                    labels.put(key);
                    createdArray.put(created.get(key));
                    resolvedArray.put(resolved.get(key));
                });
                Integer maxCreated = created.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
                Integer maxResolved = resolved.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
                if (max < maxCreated) max = maxCreated;
                if (max < maxResolved) max = maxCreated;
                IntStream.range(1, max + 1).forEach(i -> reportDTO.getTicks().add(i));
                reportDTO.setData(new JSONObject()
                        .put("labels", labels)
                        .put("data", new JSONArray().put(new JSONObject().put("data", createdArray).put("label", "Created").put("backgroundColor", colors[0]).put("borderColor", colors[0]))
                                .put(new JSONObject().put("data", resolvedArray).put("label", "Resolved").put("backgroundColor", colors[1]).put("borderColor", colors[1])))
                        .toString());
                break;
            case "rcir":
                range = "(created between '" + from + "' AND '" + to + "')";
                System.out.println(range);
                Map<String, Integer> recentlyCreated = new TreeMap<>();
                Map<String, Integer> recentlyResolved = new TreeMap<>();
                JSONArray recentlyCreatedArray = new JSONArray();
                JSONArray recentlyResolvedArray = new JSONArray();
                Map<Integer, LocalDate> rcirWeekMap = new TreeMap<>();
                for (LocalDate date = reportFrom.toLocalDate(); date.isBefore(reportDTO.getReportTo().plusDays(1)); date = date.plusDays(1)) {
                    //recentlyCreated.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                    //recentlyResolved.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                    switch (reportDTO.getGroup().toLowerCase()) {
                        case "weekly":
                            int weekOfYear = date.get(WeekFields.of(Locale.US).weekOfWeekBasedYear());
                            if (!rcirWeekMap.containsKey(weekOfYear) || rcirWeekMap.get(weekOfYear).isAfter(date)) {
                                rcirWeekMap.put(weekOfYear, date);
                            }
                            break;
                        case "monthly":
                            recentlyCreated.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-01")), 0);
                            recentlyResolved.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-01")), 0);
                            break;
                        default:
                            recentlyCreated.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                            recentlyResolved.put(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                            break;
                    }
                }
                //For weekly
                if (reportDTO.getGroup().equalsIgnoreCase("weekly")) {
                    rcirWeekMap.forEach((k, v) -> {
                        recentlyCreated.put(v.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                        recentlyResolved.put(v.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 0);
                    });
                }
                IssueSearchDTO recentlyCreatedIssues = issueService.searchIssuesLucene(projectKey, issueSearchDTO, range);
                for (Issue issue : recentlyCreatedIssues.getIssues()) {
                    String createdDate = reportDateFormat.format(issue.getCreated());
                    if (reportDTO.getGroup().equalsIgnoreCase("monthly")) {
                        createdDate = new SimpleDateFormat("yyyy-MM-01").format(issue.getCreated());
                    } else if (reportDTO.getGroup().equalsIgnoreCase("weekly")) {
                        LocalDate c = LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(issue.getCreated()));
                        int weekOfYear = c.get(WeekFields.of(Locale.US).weekOfWeekBasedYear());
                        createdDate = rcirWeekMap.containsKey(weekOfYear) ? rcirWeekMap.get(weekOfYear).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
                    }
                    if (null != issue.getResolvedDate()) {
                        if (recentlyResolved.containsKey(createdDate))
                            recentlyResolved.put(createdDate, recentlyResolved.get(createdDate) + 1);
                        else recentlyResolved.put(createdDate, 1);
                    } else if (recentlyCreated.containsKey(createdDate)) {
                        recentlyCreated.put(createdDate, recentlyCreated.get(createdDate) + 1);
                    }
                }
                recentlyCreated.forEach((key, value) -> {
                    labels.put(key);
                    recentlyCreatedArray.put(recentlyCreated.get(key));
                    recentlyResolvedArray.put(recentlyResolved.get(key));
                });
                maxCreated = recentlyCreated.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
                maxResolved = recentlyResolved.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
                if (max < maxCreated) max = maxCreated;
                if (max < maxResolved) max = maxCreated;
                IntStream.range(1, max + 1).forEach(i -> reportDTO.getTicks().add(i));
                reportDTO.setData(new JSONObject()
                        .put("labels", labels)
                        .put("data", new JSONArray().put(new JSONObject().put("data", recentlyCreatedArray).put("label", "Created").put("backgroundColor", colors[0]).put("borderColor", colors[0]))
                                .put(new JSONObject().put("data", recentlyResolvedArray).put("label", "Resolved").put("backgroundColor", colors[1]).put("borderColor", colors[1])))
                        .toString());
                break;
            case "pcr":
                range = "(created between '" + from + "' AND '" + to + "')";
                //if (!reportDTO.getQ().isEmpty()) range = range + " AND " + reportDTO.getQ();
                System.out.println(range);
                Map<String, Integer> pieData = new TreeMap<>();
                IssueSearchDTO createdIssues = issueService.searchIssuesLucene(projectKey, issueSearchDTO, range);
                for (Issue issue : createdIssues.getIssues()) {
                    switch (reportDTO.getPcrGroup()) {
                        case "assignee":
                            if (null != issue.getAssignee()) {
                                String name = issue.getAssignee().getFullName() + " [" + issue.getAssignee().getUserName() + "]";
                                if (pieData.containsKey(name))
                                    pieData.put(name, pieData.get(name) + 1);
                                else
                                    pieData.put(name, 1);
                            }
                            break;
                        case "reporter":
                            if (null != issue.getReporter()) {
                                String name = issue.getReporter().getFullName() + " [" + issue.getReporter().getUserName() + "]";
                                if (pieData.containsKey(name))
                                    pieData.put(name, pieData.get(name) + 1);
                                else
                                    pieData.put(name, 1);
                            }
                            break;
                        case "label":
                            if (null != issue.getLabels() && !issue.getLabels().isEmpty()) {
                                for (Label label : issue.getLabels()) {
                                    if (pieData.containsKey(label.getName()))
                                        pieData.put(label.getName(), pieData.get(label.getName()) + 1);
                                    else
                                        pieData.put(label.getName(), 1);
                                }
                            }
                            break;
                        case "component":
                            if (null != issue.getComponents() && !issue.getComponents().isEmpty()) {
                                for (Component component : issue.getComponents()) {
                                    if (pieData.containsKey(component.getName()))
                                        pieData.put(component.getName(), pieData.get(component.getName()) + 1);
                                    else
                                        pieData.put(component.getName(), 1);
                                }
                            }
                            break;
                        case "version":
                            if (null != issue.getVersions() && !issue.getVersions().isEmpty()) {
                                for (Version ve : issue.getVersions()) {
                                    if (pieData.containsKey(ve.getName()))
                                        pieData.put(ve.getName(), pieData.get(ve.getName()) + 1);
                                    else
                                        pieData.put(ve.getName(), 1);
                                }
                            }
                            break;
                    }
                }
                JSONArray pieSeries = new JSONArray();
                pieData.forEach((key, value) -> {
                    labels.put(key);
                    pieSeries.put(value);
                });
                reportDTO.setData(new JSONObject()
                        .put("labels", labels)
                        .put("data", new JSONArray().put(new JSONObject().put("data", pieSeries).put("label", "Created").put("backgroundColor", colors)))
                        .toString());
                break;
            case "ttr":
                JSONObject ttrData = new JSONObject();
                Long totalEstimate = 0L;
                Long totalSpent = 0L;
                JSONArray ttrSeries = new JSONArray();
                Set<WorkLog> logs = workLogService.getAllForProjectBetween(p, Timestamp.valueOf(reportFrom.toLocalDateTime())
                        , Timestamp.valueOf(reportTo.toLocalDateTime()));
                Map<String, Long> logged = new TreeMap<>();
                Map<String, Long> estimate = new TreeMap<>();
                logs.forEach(l -> {
                    String key = l.getIssue().getKeyPair();
                    if (!logged.containsKey(key))
                        logged.put(key, l.getWorkMinutes());
                    else
                        logged.put(key, l.getWorkMinutes() + logged.get(key));
                    estimate.put(key, l.getIssue().getTimeOriginalEstimate());
                });
                TimeTracking tts = projectService.getTimeTrackingSettings(p);
                for (Map.Entry<String, Long> stringLongEntry : estimate.entrySet()) {
                    String key = stringLongEntry.getKey();
                    Long estimation = stringLongEntry.getValue();
                    Long spent = logged.get(key);
                    JSONObject ttrObj = new JSONObject();
                    ttrObj.put("key", key);
                    ttrObj.put("estimate", estimation);
                    ttrObj.put("estimateString", workLogService.minutesToString(estimation, tts));
                    ttrObj.put("timeSpent", spent);
                    ttrObj.put("timeSpentString", workLogService.minutesToString(spent, tts));
                    totalEstimate += estimation;
                    totalSpent += spent;
                    ttrSeries.put(ttrObj);
                }
                ttrData.put("totalEstimate", totalEstimate);
                ttrData.put("totalSpent", totalSpent);
                ttrData.put("series", ttrSeries);
                reportDTO.setData(ttrData.toString());
                break;
        }
        return reportDTO;
    }
}
