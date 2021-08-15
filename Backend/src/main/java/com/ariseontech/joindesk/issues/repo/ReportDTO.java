package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.IssueFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Data
public class ReportDTO {

    private String group, type, pcrGroup;
    private IssueFilter filter;
    private String data, reportZone;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate reportFrom, reportTo;
    private Set<Integer> ticks;

    private Set<ReportPair> issueTypeMap, issueStepMap, issueResolutionMap, issueAssigneeMap, issueReporterMap;

    public ReportDTO() {
        issueTypeMap = new HashSet<>();
        issueStepMap = new HashSet<>();
        issueResolutionMap = new HashSet<>();
        issueAssigneeMap = new HashSet<>();
        issueReporterMap = new HashSet<>();
        ticks = new TreeSet<>();
    }

    public void addIssueType(String name, Long value) {
        issueTypeMap.add(new ReportPair(name, value));
    }

    public void addIssueStepMap(String name, Long value) {
        issueStepMap.add(new ReportPair(name, value));
    }

    public void addIssueResolutionMap(String name, Long value) {
        issueResolutionMap.add(new ReportPair(name, value));
    }

    public void addIssueAssigneeMap(String name, Long value) {
        issueAssigneeMap.add(new ReportPair(name, value));
    }

    public void addIssueReporterMap(String name, Long value) {
        issueReporterMap.add(new ReportPair(name, value));
    }

}

@Data
@AllArgsConstructor
class ReportPair {
    String name;
    Long value;
}