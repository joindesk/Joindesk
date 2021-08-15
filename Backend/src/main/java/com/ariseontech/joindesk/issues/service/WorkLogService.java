package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.WorkLog;
import com.ariseontech.joindesk.issues.repo.ReportDTO;
import com.ariseontech.joindesk.issues.repo.WorkLogRepo;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.domain.TimeTracking;
import com.ariseontech.joindesk.project.service.ProjectService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class WorkLogService {

    @Autowired
    private IssueService issueService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private WorkLogRepo workLogRepo;
    @Autowired
    private AuthService authService;

    public ReportDTO weekOverview() {
        ReportDTO reportDTO = new ReportDTO();
        reportDTO.setReportZone(authService.currentLogin().getTimezone().getID());
        ZoneId zone = ZoneId.of(reportDTO.getReportZone());
        final DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
        final DayOfWeek lastDayOfWeek = DayOfWeek.of(((firstDayOfWeek.getValue() + 5) % DayOfWeek.values().length) + 1);
        reportDTO.setReportFrom(LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(firstDayOfWeek)));
        reportDTO.setReportTo(LocalDate.now(zone).with(TemporalAdjusters.nextOrSame(lastDayOfWeek)));
        ZonedDateTime reportFrom = LocalDateTime.of(reportDTO.getReportFrom(), LocalTime.of(0, 0, 0)).atZone(zone);
        ZonedDateTime reportTo = LocalDateTime.of(reportDTO.getReportTo(), LocalTime.of(23, 0, 0)).atZone(zone);
        reportDTO.setReportFrom(reportFrom.toLocalDate());
        reportDTO.setReportTo(reportTo.toLocalDate());
        ZoneId zoneUTC = ZoneId.of("UTC");
        reportFrom = reportFrom.withZoneSameInstant(zoneUTC);
        reportTo = reportTo.withZoneSameInstant(zoneUTC);
        Map<String, Long> ttrData = new HashMap<>();
        workLogRepo.findByByAndWorkFromBetween(authService.currentLogin(), Timestamp.valueOf(reportFrom.toLocalDateTime())
                , Timestamp.valueOf(reportTo.toLocalDateTime())).forEach(log -> {
            String day = log.getWorkFrom().toLocalDateTime().getDayOfWeek().name();
            if (ttrData.containsKey(day))
                ttrData.put(day, ttrData.get(day) + log.getWorkMinutes());
            else
                ttrData.put(day, log.getWorkMinutes());
        });
        JSONObject series = new JSONObject();
        ttrData.forEach(series::put);
        reportDTO.setData(series.toString());
        return reportDTO;
    }

    public Set<WorkLog> report(String start, String end) {
        ReportDTO reportDTO = new ReportDTO();
        reportDTO.setReportZone(authService.currentLogin().getTimezone().getID());
        ZoneId zone = ZoneId.of(reportDTO.getReportZone());
        ZonedDateTime reportFrom = LocalDateTime.of(LocalDate.parse(start), LocalTime.of(0, 0, 0)).atZone(zone);
        ZonedDateTime reportTo = LocalDateTime.of(LocalDate.parse(end), LocalTime.of(23, 0, 0)).atZone(zone);
        reportDTO.setReportFrom(reportFrom.toLocalDate());
        reportDTO.setReportTo(reportTo.toLocalDate());
        ZoneId zoneUTC = ZoneId.of("UTC");
        reportFrom = reportFrom.withZoneSameInstant(zoneUTC);
        reportTo = reportTo.withZoneSameInstant(zoneUTC);
        return workLogRepo.findByByAndWorkFromBetween(authService.currentLogin(), Timestamp.valueOf(reportFrom.toLocalDateTime())
                , Timestamp.valueOf(reportTo.toLocalDateTime())).stream().peek(wl -> {
            wl.setFrom(Timestamp.valueOf(wl.getWorkFrom().toLocalDateTime()));
            wl.setTo(Timestamp.valueOf(wl.getWorkFrom().toLocalDateTime().plusMinutes(wl.getWorkMinutes())));
            TimeTracking tts = projectService.getTimeTrackingSettings(wl.getIssue().getProject());
            wl.setWork(minutesToString(wl.getWorkMinutes(), tts));
        }).collect(Collectors.toSet());
    }

    public Set<WorkLog> getAllForProject(String projectKey, Long issueKey) {
        Issue issue = issueService.get(projectKey, issueKey);
        Set<WorkLog> workLogs = new HashSet<>();
        TimeTracking tts = projectService.getTimeTrackingSettings(issue.getProject());
        workLogRepo.findByIssue(issue).forEach(w -> {
            w.setEditable(canEdit(w));
            w.setWork(minutesToString(w.getWorkMinutes(), tts));
            workLogs.add(w);
        });
        return workLogs;
    }

    public Set<WorkLog> getAllForProjectBetween(Project project, Timestamp from, Timestamp to) {
        return workLogRepo.findByProjectAndWorkFromBetween(project, from, to);
    }

    public long getWorkLoggedForIssue(Issue issue) {
        Long v = workLogRepo.sumOfWorkLoggedByIssue(issue);
        if (v == null) {
            return 0L;
        }
        return v;
    }

    public String minutesToString(Long workMinutes, TimeTracking tts) {
        StringBuilder sb = new StringBuilder();
        final int HOUR = 60;
        final int DAY = tts.getHoursPerDay() * HOUR;
        final int WEEK = tts.getDaysPerWeek() * DAY;
        int weeks = 0, days = 0, hours = 0, minutes = 0;
        while (workMinutes > 0) {
            if (workMinutes >= WEEK) {
                weeks++;
                workMinutes = workMinutes - WEEK;
            } else if (workMinutes >= DAY) {
                days++;
                workMinutes = workMinutes - DAY;
            } else if (workMinutes >= HOUR) {
                hours++;
                workMinutes = workMinutes - HOUR;
            } else {
                minutes = Math.toIntExact(workMinutes);
                workMinutes = 0L;
            }
        }
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        return sb.toString();
    }

    public Long stringToMinutes(String estimateString, TimeTracking tts) {
        final int HOUR = 60;
        final int DAY = tts.getHoursPerDay() * HOUR;
        final int WEEK = tts.getDaysPerWeek() * DAY;
        String[] ww = estimateString.split(" ");
        AtomicLong minutes = new AtomicLong();
        Arrays.stream(ww).forEach(www -> {
            if (www.endsWith("h")) {
                minutes.set(minutes.get() + (Long.parseLong(www.substring(0, www.indexOf("h"))) * HOUR));
            } else if (www.endsWith("d")) {
                minutes.set(minutes.get() + (Long.parseLong(www.substring(0, www.indexOf("d"))) * DAY));
            } else if (www.endsWith("m")) {
                minutes.set(minutes.get() + Long.parseLong(www.substring(0, www.indexOf("m"))));
            } else if (www.endsWith("w")) {
                minutes.set(minutes.get() + Long.parseLong(www.substring(0, www.indexOf("w"))) * WEEK);
            } else {
                //default to minutes
                minutes.set(minutes.get() + Long.parseLong(www));
            }
        });
        return minutes.get();
    }

    public WorkLog save(String projectKey, Long issueKey, WorkLog workLogToSave) {
        Issue issue = issueService.get(projectKey, issueKey);
        if (!projectService.getTimeTrackingSettings(issue.getProject()).isEnabled())
            throw new JDException("Project doesn`t allow time tracking", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        workLogToSave.setIssue(issue);
        workLogToSave.setProject(issue.getProject());
        workLogToSave.setWorkMinutes(stringToMinutes(workLogToSave.getWork(), projectService.getTimeTrackingSettings(issue.getProject())));
        String work = workLogToSave.getWork();
        if (workLogToSave.getId() != null && !canEdit(workLogToSave))
            throw new JDException("Edit only own logs", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        workLogToSave.setBy(authService.currentLogin());
        workLogToSave = workLogRepo.save(workLogToSave);
        workLogToSave.setWork(work);
        return workLogToSave;
    }

    public void delete(String projectKey, Long issueKey, Long workLogID) {
        Optional<WorkLog> w = workLogRepo.findById(workLogID);
        if (w.isEmpty()) throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!canEdit(w.get()))
            throw new JDException("Can only delete own logs", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (!w.get().getProject().getKey().equals(projectKey))
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        if (!w.get().getIssue().getKey().equals(issueKey))
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        workLogRepo.delete(w.get());
    }

    public void deleteAll(Issue issue) {
        workLogRepo.findByIssue(issue).forEach(w -> workLogRepo.delete(w));

    }

    public WorkLog getWorkLog(String projectKey, Long issueKey, Long workLogID) {
        Optional<WorkLog> log = workLogRepo.findById(workLogID);
        if (log.isEmpty())
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!log.get().getProject().getKey().equals(projectKey))
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        if (!log.get().getIssue().getKey().equals(issueKey))
            throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        TimeTracking tts = projectService.getTimeTrackingSettings(log.get().getProject());
        log.get().setWork(minutesToString(log.get().getWorkMinutes(), tts));
        return log.get();
    }

    public boolean canEdit(WorkLog workLog) {
        return workLog.getBy().equals(authService.currentLogin());
    }
}
