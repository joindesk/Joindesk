package com.ariseontech.joindesk.auth.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.auth.domain.IPWhiteBlacklist;
import com.ariseontech.joindesk.auth.service.IPWhiteBlackListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping(produces = "application/json", consumes = "application/json", value = SystemInfo.apiPrefix + "/manage/access/")
public class IPRangeController {

    @Autowired
    private IPWhiteBlackListingService ipWhiteBlackListingService;

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public String getAll() {
        return HelperUtil.squiggly("base", ipWhiteBlackListingService.getAll());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/save")
    public String save(@Valid @RequestBody IPWhiteBlacklist l) {
        return HelperUtil.squiggly("base,user_detail,user_detail_admin", ipWhiteBlackListingService.save(l));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/delete")
    public void delete(@RequestBody IPWhiteBlacklist l) {
        ipWhiteBlackListingService.delete(l);
    }
}
