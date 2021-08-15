package com.ariseontech.joindesk.webhook.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.event.domain.JDEventType;
import com.ariseontech.joindesk.webhook.domain.WebHook;
import com.ariseontech.joindesk.webhook.service.WebHookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/manage/webhook", produces = "application/json", consumes = "application/json")
public class WebHookController {

    @Autowired
    private WebHookService webHookService;

    @GetMapping("/")
    public String getAll() {
        return HelperUtil.squiggly("-user_detail", webHookService.getAllHooks());
    }

    @GetMapping("{id}")
    public String get(@PathVariable("id") Long id) {
        return HelperUtil.squiggly("-user_details", webHookService.getHook(id));
    }

    @RequestMapping(method = RequestMethod.POST, value = "save")
    public String save(@RequestBody WebHook v) {
        return HelperUtil.squiggly("base,-user_detail", webHookService.save(v));
    }

    @RequestMapping(method = RequestMethod.POST, value = "delete")
    public void remove(@RequestBody WebHook v) {
        webHookService.delete(v);
    }

    @GetMapping("/eventTypes")
    public String getEventTypes() {
        return HelperUtil.squiggly("-user_detail", JDEventType.values());
    }
}
