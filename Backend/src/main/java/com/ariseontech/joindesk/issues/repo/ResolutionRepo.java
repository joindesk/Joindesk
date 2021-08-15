package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolutionRepo extends JpaRepository<Resolution, Long> {
    Resolution findByName(String name);
}
