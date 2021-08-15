package com.ariseontech.joindesk.project.service;

import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.ErrorDetails;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.project.domain.GlobalGroup;
import com.ariseontech.joindesk.project.domain.Group;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.repo.GlobalGroupRepo;
import com.ariseontech.joindesk.project.repo.GroupRepo;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupRepo groupRepo;
    @Autowired
    private GlobalGroupRepo globalGroupRepo;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private Validator validator;

    public boolean hasGlobalManageAccess() {
        return projectService.hasGlobalManageAccess();
    }

    public boolean hasGlobalViewAccess() {
        return projectService.hasGlobalViewAccess();
    }

    public boolean hasProjectManageAccess(Project p) {
        return projectService.hasProjectManageAccess(p);
    }

    public boolean hasProjectViewAccess(Project p) {
        return projectService.hasProjectViewAccess(p);
    }

    public Set<Group> getAllGroupsForProject(String projectKey) {
        Optional<Project> p = Optional.ofNullable(projectService.findByKey(projectKey));
        if (p.isEmpty())
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectViewAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<Group> groups = groupRepo.findByProjectOrderByNameAsc(p.get());
        groups.forEach(g -> g.setEditable(hasProjectManageAccess(p.get())));
        return groups;
    }

    public Group getGroupForProject(String projectKey, Long groupID) {
        Optional<Project> p = Optional.ofNullable(projectService.findByKey(projectKey));
        if (p.isEmpty())
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectViewAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Group g = groupRepo.findByProjectAndId(p.get(), groupID);
        if (null == g)
            throw new JDException("", ErrorCode.GROUP_NOT_FOUND, HttpStatus.NOT_FOUND);
        g.setEditable(hasProjectManageAccess(p.get()));
        return g;
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public Group createGroup(String projectKey, Group group) {
        Optional<Project> p = Optional.ofNullable(projectService.findByKey(projectKey));
        if (p.isEmpty())
            throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectManageAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        group.setProject(p.get());
        Set<ConstraintViolation<Group>> result = validator.validate(group);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED);
            error.setErrors(details);
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        groupRepo.save(group);
        syncGroup(group);
        return group;
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public Group updateProject(String projectKey, Long groupId, String field, String value) {
        Optional<Project> p = Optional.ofNullable(projectService.findByKey(projectKey));
        if (p.isEmpty()) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        Group g = groupRepo.findByProjectAndId(p.get(), groupId);
        Login syncLogin = null;
        Group syncGroup = null;
        if (null == g)
            throw new JDException("", ErrorCode.GROUP_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectManageAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        switch (field) {
            case "add_user":
                Login add_user = userService.find(Long.parseLong(value));
                g.getUsers().add(add_user);
                syncLogin = add_user;
                break;
            case "remove_user":
                Login remove_user = userService.find(Long.parseLong(value));
                Set<Login> users = g.getUsers();
                users = users.stream().filter(u -> !u.getId().equals(remove_user.getId())).collect(Collectors.toSet());
                g.setUsers(users);
                syncLogin = remove_user;
                break;
            case "add_authority":
                try {
                    g.getAuthorityCodes().add(AuthorityCode.valueOf(value));
                    syncGroup = g;
                } catch (IllegalArgumentException e) {
                    throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                }
                break;
            case "remove_authority":
                try {
                    g.getAuthorityCodes().remove(AuthorityCode.valueOf(value));
                    syncGroup(g);
                } catch (IllegalArgumentException e) {
                    throw new JDException("", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
                }
                break;
        }
        Set<ConstraintViolation<Group>> result = validator.validate(g);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED);
            error.setErrors(details);
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        groupRepo.save(g);
        if (null != syncGroup)
            syncGroup(syncGroup);
        if (null != syncLogin)
            syncLogin(syncLogin);
        g.setEditable(true);
        return g;
    }

    public AuthorityCode[] getAuthorityCodes() {
        return AuthorityCode.values();
    }

    private void syncLogin(Login l) {
        authService.calculateAuthoritiesForLogin(l);
    }

    private void syncGroup(Group g) {
        Set<Login> users = g.getUsers();
        if (g.isAllUsers())
            users.addAll(userService.getAll());
        users.forEach(this::syncLogin);
    }

    private void syncGlobalGroup(GlobalGroup g) {
        g.getUsers().forEach(this::syncLogin);
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public Group updateGroupDetails(String projectKey, Long groupId, Group grp) {
        Optional<Project> p = Optional.ofNullable(projectService.findByKey(projectKey));
        if (p.isEmpty()) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        Group g = groupRepo.findByProjectAndId(p.get(), groupId);
        if (null == g) throw new JDException("", ErrorCode.GROUP_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectManageAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<Login> orgUsrs = g.getUsers();
        g.setUsers(grp.getUsers());
        g.setAuthorityCodes(grp.getAuthorityCodes());
        g.setAllUsers(grp.isAllUsers());
        if (!grp.getName().equals(g.getName()))
            g.setName(grp.getName());
        Set<ConstraintViolation<Group>> result = validator.validate(g);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        groupRepo.save(g);
        syncGroup(g);
        //Sync original group to remove users
        orgUsrs.forEach(this::syncLogin);
        g.setEditable(true);
        return g;
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public void removeGroup(String projectKey, Long groupId, Group grp) {
        Optional<Project> p = Optional.ofNullable(projectService.findByKey(projectKey));
        if (p.isEmpty()) throw new JDException("", ErrorCode.PROJECT_NOT_FOUND, HttpStatus.NOT_FOUND);
        Group g = groupRepo.findByProjectAndId(p.get(), groupId);
        if (null == g) throw new JDException("", ErrorCode.GROUP_NOT_FOUND, HttpStatus.NOT_FOUND);
        if (!hasProjectManageAccess(p.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<Login> members = g.getUsers();
        groupRepo.delete(g);
        members.forEach(this::syncLogin);
    }

    /* Global Group */
    public List<GlobalGroup> getAllGlobalGroups() {
        if (!authService.isSuperAdmin())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return globalGroupRepo.findAll();
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public GlobalGroup saveGlobalGroup(GlobalGroup globalGroup) {
        if (!authService.isSuperAdmin())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<GlobalGroup> orgGroup = Optional.empty();
        Set<ConstraintViolation<GlobalGroup>> result = validator.validate(globalGroup);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            ErrorDetails error = new ErrorDetails(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED);
            error.setErrors(details);
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        //If already exists
        if (globalGroup.getId() != null) {
            Optional<GlobalGroup> webH = globalGroupRepo.findById(globalGroup.getId());
            orgGroup = webH;
            if (webH.isPresent()) {
                globalGroup.setCreated(webH.get().getCreated());
                globalGroup.setCreatedBy(webH.get().getCreatedBy());
            }
        }
        globalGroupRepo.save(globalGroup);
        syncGlobalGroup(globalGroup);
        //Sync original group to remove users
        orgGroup.ifPresent(g -> g.getUsers().forEach(this::syncLogin));
        return globalGroup;
    }

    @CacheEvict(value = "projectMembers", allEntries = true)
    public void removeGlobalGroup(GlobalGroup g) {
        if (!authService.isSuperAdmin())
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<GlobalGroup> gg = globalGroupRepo.findById(g.getId());
        if (gg.isPresent()) {
            Set<Login> members = gg.get().getUsers();
            globalGroupRepo.delete(gg.get());
            members.forEach(this::syncLogin);
        }
    }

}
