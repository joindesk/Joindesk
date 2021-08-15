package com.ariseontech.joindesk.scheduler.repo;

import com.ariseontech.joindesk.scheduler.domain.JDScheduler;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JDSchedulerRepo extends JpaRepository<JDScheduler, Long> {

    JDScheduler findByJobID(String id);

}