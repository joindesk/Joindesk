package com.ariseontech.joindesk.scheduler;

import com.ariseontech.joindesk.scheduler.service.JDSchedulerService;
import lombok.extern.java.Log;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Log
@Service
public class JDJobRefresher {
    @Autowired
    private JDSchedulerService jdSchedulerService;

    //Run everyday at 23:00 hours
    @Scheduled(cron = "0 0 23 * * ?")
    public void reloadallJobs() {
        try {
            jdSchedulerService.refreshScheduler();
        } catch (SchedulerException | ParseException e) {
            e.printStackTrace();
        }
    }
}
