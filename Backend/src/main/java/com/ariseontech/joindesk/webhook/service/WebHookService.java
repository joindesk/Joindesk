package com.ariseontech.joindesk.webhook.service;

import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.webhook.domain.WebHook;
import com.ariseontech.joindesk.webhook.repo.WebHookLogRepo;
import com.ariseontech.joindesk.webhook.repo.WebHookRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WebHookService {

    @Autowired
    private WebHookRepo webHookRepo;
    @Autowired
    private IssueService issueService;
    @Autowired
    private AuthService authService;
    @Autowired
    private WebHookLogRepo webHookLogRepo;

    public List<WebHook> getAllHooks() {
        checkManageAccess();
        return webHookRepo.findAll();
    }

    public Optional<WebHook> getHook(Long id) {
        checkManageAccess();
        return webHookRepo.findById(id);
    }

    public WebHook save(WebHook webHook) {
        checkManageAccess();
        //If already exists
        if (webHook.getId() != null) {
            Optional<WebHook> webH = webHookRepo.findById(webHook.getId());
            if (webH.isPresent()) {
                webHook.setCreated(webH.get().getCreated());
                webHook.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        webHook = webHookRepo.save(webHook);
        return webHook;
    }

    public void delete(WebHook webHook) {
        checkManageAccess();
        webHookLogRepo.deleteInBatch(webHookLogRepo.findByWebhookId(webHook.getId()));
        webHookRepo.delete(webHook);
    }

    public boolean hasManageAccess() {
        return authService.isSuperAdmin();
    }

    private void checkManageAccess() {
        if (!hasManageAccess()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }
}
