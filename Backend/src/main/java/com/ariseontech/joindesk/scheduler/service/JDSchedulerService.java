package com.ariseontech.joindesk.scheduler.service;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.scheduler.JDScheduledJobRunner;
import com.ariseontech.joindesk.scheduler.domain.JDScheduler;
import com.ariseontech.joindesk.scheduler.repo.JDSchedulerRepo;
import lombok.extern.java.Log;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.quartz.TriggerBuilder.newTrigger;

@Service
@Log
public class JDSchedulerService {

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private JDSchedulerRepo jdSchedulerRepo;

    public List<JobKey> getJobs() throws SchedulerException {
        List<JobKey> jobs = new ArrayList<>();
        for (String gn : scheduler.getJobGroupNames()) {
            jobs.addAll(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(gn)));
        }
        return jobs;
    }

    public JDScheduler save(JDScheduler jdScheduler) throws SchedulerException, ParseException {
        //If already exists
        if (jdScheduler.getId() != null) {
            Optional<JDScheduler> webH = jdSchedulerRepo.findById(jdScheduler.getId());
            if (webH.isPresent()) {
                jdScheduler.setCreated(webH.get().getCreated());
                jdScheduler.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        jdScheduler = jdSchedulerRepo.save(jdScheduler);
        refreshScheduler();
        return jdScheduler;
    }

    private void schedule(JDScheduler jdScheduler) throws ParseException, SchedulerException {
        if (!jdScheduler.isActive()) return;
        //Schedule only active jobs
        JobDetail jobDetail = buildJobDetail(jdScheduler);
        Trigger trigger = null;
        trigger = buildJobTrigger(jobDetail, jdScheduler);
        jdScheduler.setJobKey(jobDetail.getKey().getName());
        jdSchedulerRepo.save(jdScheduler);
        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void refreshScheduler() throws SchedulerException, ParseException {
        //All jobs cleared
        scheduler.clear();
        for (JDScheduler s : jdSchedulerRepo.findAll()) {
            schedule(s);
        }
    }

    public void scheduleDueDateJob() throws ParseException, SchedulerException {
        String tenant = "job";
        String jobId = tenant + "_duedatenotifier";
        ZoneId timezone = ZoneId.of(configurationService.getString(ConfigurationService.JDCONFIG.APP_TIMEZONE));
        LocalTime lt = LocalTime.parse(configurationService.getString(ConfigurationService.JDCONFIG.APP_BUSINESS_START_TIME));
        if (timezone == null) timezone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime zoneTime = ZonedDateTime.of(lt.atDate(LocalDate.now()), timezone);
        ZonedDateTime utcDate = zoneTime.withZoneSameInstant(ZoneOffset.UTC);
        String cronExp = HelperUtil.generateCronExpression(String.valueOf(0), String.valueOf(utcDate.getMinute()), String.valueOf(utcDate.getHour()), "?", "*", "*", "*");
        JDScheduler sj = jdSchedulerRepo.findByJobID(jobId);
        if (sj == null)
            sj = new JDScheduler(jobId, tenant + "duedate", tenant + "-due-date-job", tenant + " due date job", cronExp);
        else sj.setCronExpression(cronExp);
        sj.setStartAt(new Date());
        save(sj);
    }

    private JobDetail buildJobDetail(JDScheduler jdScheduler) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email", "");
        return JobBuilder.newJob(JDScheduledJobRunner.class)
                .withIdentity(jdScheduler.getJobID(), jdScheduler.getJobGroup())
                .withDescription(jdScheduler.getJobDescription())
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, JDScheduler jdScheduler) throws ParseException {
        return newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), jdScheduler.getJobGroup() + "-trigger")
                .withDescription(jdScheduler.getJobDescription() + " trigger")
                .startAt(jdScheduler.getStartAt())
                .withSchedule(CronScheduleBuilder.cronScheduleNonvalidatedExpression(jdScheduler.getCronExpression()))
                .build();
    }

}
