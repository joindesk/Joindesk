package com.ariseontech.joindesk.issues.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.issues.domain.Relationship;
import com.ariseontech.joindesk.issues.domain.Resolution;
import com.ariseontech.joindesk.issues.repo.ResolutionRepo;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.issues.service.RelationshipService;
import com.ariseontech.joindesk.project.domain.GlobalGroup;
import com.ariseontech.joindesk.project.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(value = SystemInfo.apiPrefix + "/manage")
public class ManageController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private GroupService groupService;

    //Relationship manage
    @RequestMapping(method = RequestMethod.GET, value = "relationships")
    public String getRelationshipTypes() {
        return HelperUtil.squiggly("base", relationshipService.getAll());
    }

    @RequestMapping(method = RequestMethod.POST, value = "relationships/save")
    public String saveRelationship(@RequestBody Relationship relationship) {
        return HelperUtil.squiggly("base", relationshipService.saveRelationship(relationship));
    }

    @RequestMapping(method = RequestMethod.POST, value = "relationships/remove")
    public void removeRelationship(@RequestBody Relationship relationship) {
        relationshipService.removeRelationShip(relationship);
    }

    //Resolution Manage
    @RequestMapping(method = RequestMethod.GET, value = "resolutions")
    public String getResolution() {
        return HelperUtil.squiggly("base", resolutionRepo.findAll());
    }

    @RequestMapping(method = RequestMethod.POST, value = "resolutions/save")
    public String saveResolution(@RequestBody Resolution resolution) {
        //If already exists
        if (resolution.getId() != null) {
            Optional<Resolution> webH = resolutionRepo.findById(resolution.getId());
            if (webH.isPresent()) {
                resolution.setCreated(webH.get().getCreated());
                resolution.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        return HelperUtil.squiggly("base", resolutionRepo.save(resolution));
    }

    //Group management
    @RequestMapping(method = RequestMethod.GET, value = "group")
    public String getGroups() {
        return HelperUtil.squiggly("base,-user_detail", groupService.getAllGlobalGroups());
    }

    @RequestMapping(method = RequestMethod.POST, value = "group/save")
    public String saveGroup(@RequestBody GlobalGroup g) {
        return HelperUtil.squiggly("base,-user_detail", groupService.saveGlobalGroup(g));
    }

    @RequestMapping(method = RequestMethod.POST, value = "group/remove")
    public void removeGroup(@RequestBody GlobalGroup g) {
        groupService.removeGlobalGroup(g);
    }
}
