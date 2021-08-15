package com.ariseontech.joindesk.project.repo;

import com.ariseontech.joindesk.project.domain.JDConfiguration;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JDConfigurationRepo extends JpaRepository<JDConfiguration, Long> {
    JDConfiguration findByKey(ConfigurationService.JDCONFIG key);
}
