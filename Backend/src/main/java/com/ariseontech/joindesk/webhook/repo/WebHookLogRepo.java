package com.ariseontech.joindesk.webhook.repo;

import com.ariseontech.joindesk.webhook.domain.WebHookLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface WebHookLogRepo extends JpaRepository<WebHookLog, Long> {

    Set<WebHookLog> findByWebhookId(Long id);

}