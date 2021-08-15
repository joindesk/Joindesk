package com.ariseontech.joindesk.auth.repo;


import com.ariseontech.joindesk.auth.domain.IPWhiteBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface IPWhiteBlackListRepo extends JpaRepository<IPWhiteBlacklist, Long> {

    Set<IPWhiteBlacklist> findByEnabledTrue();

    Set<IPWhiteBlacklist> findByEnabledTrueAndWhiteListTrue();

    Set<IPWhiteBlacklist> findByEnabledTrueAndWhiteListFalse();

}
