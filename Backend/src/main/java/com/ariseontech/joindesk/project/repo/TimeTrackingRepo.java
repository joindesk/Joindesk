package com.ariseontech.joindesk.project.repo;

import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.domain.TimeTracking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeTrackingRepo extends JpaRepository<TimeTracking, Long> {

    TimeTracking findByProject(Project project);


}
