package com.ariseontech.joindesk.event.repo;

import com.ariseontech.joindesk.event.domain.JDEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JDEventRepo extends JpaRepository<JDEvent, Long> {

}