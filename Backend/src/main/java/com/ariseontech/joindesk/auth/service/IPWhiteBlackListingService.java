package com.ariseontech.joindesk.auth.service;

import com.ariseontech.joindesk.auth.domain.IPWhiteBlacklist;
import com.ariseontech.joindesk.auth.repo.IPWhiteBlackListRepo;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.project.service.ProjectService;
import lombok.extern.java.Log;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Log
public class IPWhiteBlackListingService {

    @Autowired
    private LoginRepo loginRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private CurrentLogin currentLogin;
    @Autowired
    private IPWhiteBlackListRepo ipWhiteBlackListRepo;

    @Cacheable(value = "allowedWeb", key = "#ip")
    public boolean isAllowedWeb(String ip) {
        Set<IPWhiteBlacklist> rules = ipWhiteBlackListRepo.findByEnabledTrue().stream().filter(r -> !r.isApiOnly()).collect(Collectors.toSet());
        //Check for blacklist and if matched deny access
        log.info("IP:" + ip);
        boolean blackListed = rules.stream().anyMatch(r -> !r.isWhiteList() && new SubnetUtils(r.getCidr()).getInfo().isInRange(ip));
        log.info("Blacklisted :" + blackListed);
        if (blackListed) return false;
        //If whitelist is not enabled, allow all
        boolean whiteListEnabled = rules.stream().anyMatch(IPWhiteBlacklist::isWhiteList);
        log.info("WhiteList Enabled :" + whiteListEnabled);
        if (!whiteListEnabled) return true;
        //Else allow only those whitelisted
        return rules.stream().anyMatch(r -> r.isWhiteList() && new SubnetUtils(r.getCidr()).getInfo().isInRange(ip));
    }

    @Cacheable(value = "allowedIP", key = "#ip")
    public boolean isAllowedApi(String ip) {
        Set<IPWhiteBlacklist> rules = ipWhiteBlackListRepo.findByEnabledTrue().stream().filter(IPWhiteBlacklist::isApiOnly).collect(Collectors.toSet());
        //Check for blacklist and if matched deny access
        log.info("IP:" + ip);
        boolean blackListed = rules.stream().anyMatch(r -> !r.isWhiteList() && new SubnetUtils(r.getCidr()).getInfo().isInRange(ip));
        log.info("Blacklisted :" + blackListed);
        if (blackListed) return false;
        //If whitelist is not enabled, allow all
        boolean whiteListEnabled = rules.stream().anyMatch(IPWhiteBlacklist::isWhiteList);
        log.info("WhiteList Enabled :" + whiteListEnabled);
        if (!whiteListEnabled) return true;
        //Else allow only those whitelisted
        return rules.stream().anyMatch(r -> r.isWhiteList() && new SubnetUtils(r.getCidr()).getInfo().isInRange(ip));
    }

    public List<IPWhiteBlacklist> getAll() {
        checkManageAccess();
        return ipWhiteBlackListRepo.findAll();
    }

    @Caching(evict = {
            @CacheEvict(value = "allowedWeb", allEntries = true),
            @CacheEvict(value = "allowedIP", allEntries = true),
    })
    public IPWhiteBlacklist save(IPWhiteBlacklist ipWhiteBlacklist) {
        checkManageAccess();
        try {
            new SubnetUtils(ipWhiteBlacklist.getCidr());
        } catch (Exception e) {
            throw new JDException("Invalid CIDR", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        return ipWhiteBlackListRepo.save(ipWhiteBlacklist);
    }

    @Caching(evict = {
            @CacheEvict(value = "allowedWeb", allEntries = true),
            @CacheEvict(value = "allowedIP", allEntries = true),
    })
    public void delete(IPWhiteBlacklist ipWhiteBlacklist) {
        checkManageAccess();
        ipWhiteBlackListRepo.delete(ipWhiteBlacklist);
    }

    public boolean hasManageAccess() {
        return authService.isSuperAdmin();
    }

    private void checkManageAccess() {
        if (!hasManageAccess()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

}
