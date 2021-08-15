package com.ariseontech.joindesk.event.repo;

import com.ariseontech.joindesk.event.domain.JDEventType;
import com.ariseontech.joindesk.event.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    List<Subscription> findByLoginID(Long loginID);

    List<Subscription> findByLoginIDAndTypeAndProjectKey(Long loginID, JDEventType type, String projectKey);

    List<Subscription> findByTypeAndProjectKey(JDEventType type, String projectKey);

    Subscription findByIdAndTypeAndProjectKey(Long id, JDEventType type, String projectKey);

}