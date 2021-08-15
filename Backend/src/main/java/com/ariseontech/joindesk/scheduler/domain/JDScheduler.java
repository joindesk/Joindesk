package com.ariseontech.joindesk.scheduler.domain;

import com.ariseontech.joindesk.AuditModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Data
@Entity
@EqualsAndHashCode(callSuper = false)
public class JDScheduler extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String jobID, jobKey;
    private String jobType, jobGroup, jobDescription, cronExpression;

    @Column(columnDefinition = "boolean default true")
    private boolean active;

    private Date startAt;

    @OneToMany(fetch = FetchType.EAGER)
    private List<JDSchedulerJobData> data;

    public JDScheduler(String jobID, String jobType, String jobGroup, String jobDescription, String cronExpression) {
        this.jobID = jobID;
        this.jobType = jobType;
        this.jobGroup = jobGroup;
        this.jobDescription = jobDescription;
        this.cronExpression = cronExpression;
        this.data = new ArrayList<>();
        this.active = true;
        this.startAt = new Date();
    }

    public JDScheduler(String jobID, String jobType, String jobGroup, String jobDescription, String cronExpression, List<JDSchedulerJobData> data) {
        this.jobID = jobID;
        this.jobType = jobType;
        this.jobGroup = jobGroup;
        this.jobDescription = jobDescription;
        this.cronExpression = cronExpression;
        this.data = data;
        this.active = true;
        this.startAt = new Date();
    }
}

@Entity
@Data
class JDSchedulerJobData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String key, value;
}
