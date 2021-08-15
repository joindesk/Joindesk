package com.ariseontech.joindesk.webhook.repo;

import com.ariseontech.joindesk.webhook.domain.WebHook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface WebHookRepo extends JpaRepository<WebHook, Long> {
    Set<WebHook> findByActiveTrue();
}