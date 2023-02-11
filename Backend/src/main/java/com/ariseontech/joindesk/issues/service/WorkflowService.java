package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.*;
import com.ariseontech.joindesk.project.domain.Group;
import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.project.repo.GroupRepo;
import com.ariseontech.joindesk.project.service.GroupService;
import com.ariseontech.joindesk.project.service.ProjectService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    @Autowired
    private AuthService authService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private IssueRepo issueRepo;

    @Autowired
    private LoginRepo loginRepo;

    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private ResolutionRepo resolutionRepo;

    @Autowired
    private IssueTypeService issueTypeService;

    @Autowired
    private WorkflowRepo workflowRepo;

    @Autowired
    private WorkflowStepRepo workflowStepRepo;

    @Autowired
    private WorkflowTransitionRepo workflowTransitionRepo;

    @Autowired
    private WorkflowTransitionPropertyRepo workflowTransitionPropertyRepo;

    @Autowired
    private Validator validator;

    @Autowired
    private IssueService issueService;

    public Optional<Workflow> findWorkflow(Long workflowID) {
        Optional<Workflow> workflow = workflowRepo.findById(workflowID);
        if (workflow.isEmpty()) throw new JDException("", ErrorCode.WORKFLOW_NOT_FOUND, HttpStatus.NOT_FOUND);
//        if (!hasWorkflowManageAccess(workflow.get()))
//            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        workflow.get().setWorkflowTransitions(getWorkflowTransitions(workflow.get()));
        workflow.get().setWorkflowSteps(getWorkflowSteps(workflow.get()));
        workflow.get().setEditable(true);
        List<WorkflowStepTransitionDTO> workflowStepTransitions = new ArrayList<>();
        workflow.get().getWorkflowSteps().stream().sorted(Comparator.comparing(p -> p.getIssueStatus().getName())).forEach(ws -> {
            WorkflowStepTransitionDTO workflowStepTransition = new WorkflowStepTransitionDTO();
            workflowStepTransition.setStep(ws);
            List<WorkflowTransition> transitions = new ArrayList<>();
            workflow.get().getWorkflowTransitions()
                    .forEach(wt -> {
                        if (wt.isFromAll()) {
                            WorkflowTransition transition = new WorkflowTransition();
                            transition.setId(wt.getId());
                            transition.setFromStep(ws);
                            transition.setToStep(wt.getToStep());
                            transition.setName(wt.getName());
                            transitions.add(transition);
                        } else if (wt.getFromStep() != null && wt.getFromStep().equals(ws)) {
                            transitions.add(wt);
                        }
                    });
            workflowStepTransition.setWorkflowTransitions(transitions);
            workflowStepTransitions.add(workflowStepTransition);
        });
        workflow.get().setWorkflowStepTransitions(workflowStepTransitions);
        return workflow;
    }

    public boolean hasWorkflowManageAccess(Workflow workflow) {
        return projectService.hasProjectManageAccess(workflow.getProject());
    }

    public Workflow create(String name, String desc, Long projectID) {
        Project project = projectService.getProject(projectID);
        Workflow w = new Workflow(name, project);
        w.setDescription(desc);
        return create(w);
    }

    public Workflow create(Workflow workflow) {
        if (!projectService.hasProjectManageAccess(workflow.getProject()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        if (workflow.getId() != null)
            throw new JDException("", ErrorCode.BAD_REQUEST, HttpStatus.PRECONDITION_FAILED);
        Set<ConstraintViolation<Workflow>> result = validator.validate(workflow);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        workflow = workflowRepo.save(workflow);
        //Create sample steps including default steps
        IssueStatus todoStatus = issueTypeService.saveStatus(new IssueStatus("TODO", IssueLifeCycle.TODO));
        IssueStatus inProgressStatus = issueTypeService.saveStatus(new IssueStatus("IN PROGRESS", IssueLifeCycle.INPROGRESS));
        IssueStatus doneStatus = issueTypeService.saveStatus(new IssueStatus("DONE", IssueLifeCycle.DONE));
        WorkflowStep todoState = new WorkflowStep(todoStatus, workflow);
        WorkflowStep inProgressState = new WorkflowStep(inProgressStatus, workflow);
        WorkflowStep doneState = new WorkflowStep(doneStatus, workflow);
        Set<WorkflowStep> steps = new HashSet<>();
        steps.add(todoState);
        steps.add(inProgressState);
        steps.add(doneState);
        workflowStepRepo.saveAll(steps);
        WorkflowTransition todoToInProgressTransition = new
                WorkflowTransition("Start", todoState, inProgressState, workflow);
        WorkflowTransition inProgressToDoneTransition = new WorkflowTransition("Complete", inProgressState, doneState, workflow);
        WorkflowTransition doneToTodoTransition = new WorkflowTransition("Restart", doneState, todoState, workflow);
        Set<WorkflowTransition> transitions = new HashSet<>();
        todoToInProgressTransition = workflowTransitionRepo.save(todoToInProgressTransition);
        inProgressToDoneTransition = workflowTransitionRepo.save(inProgressToDoneTransition);
        doneToTodoTransition = workflowTransitionRepo.save(doneToTodoTransition);
        workflowTransitionPropertyRepo.save(new WorkflowTransitionProperties(WorkflowTransitionPropertyTypes.POST_FUNCTION, WorkflowTransitionPropertySubTypes.POST_FUNCTION_UPDATE_FIELD, "Resolution", resolutionRepo.findByName("DONE").getId().toString(), inProgressToDoneTransition));
        workflowTransitionPropertyRepo.save(new WorkflowTransitionProperties(WorkflowTransitionPropertyTypes.POST_FUNCTION, WorkflowTransitionPropertySubTypes.POST_FUNCTION_UPDATE_FIELD, "Resolution", "-none-", doneToTodoTransition));
        transitions.add(todoToInProgressTransition);
        transitions.add(inProgressToDoneTransition);
        transitions.add(doneToTodoTransition);
        workflow.setWorkflowSteps(steps);
        workflow.setWorkflowTransitions(transitions);
        workflow.setEditable(true);
        workflow.setDefaultStep(todoState);
        return workflowRepo.save(workflow);
    }

    public void deleteWorkflow(Long workflowID) {
        Optional<Workflow> workflow = findWorkflow(workflowID);
        if (!hasWorkflowManageAccess(workflow.get()))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        List<Issue> associatedIssueTypes = issueTypeService.findByWorkflow(workflow.get());
        if (!associatedIssueTypes.isEmpty())
            throw new JDException("Workflow is active", ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        workflowTransitionRepo.deleteInBatch(workflowTransitionRepo.findByWorkflow(workflow.get()));
        workflowStepRepo.deleteInBatch(workflowStepRepo.findByWorkflow(workflow.get()));
        workflowRepo.deleteById(workflow.get().getId());
    }

    public List<Workflow> getAll(String projectKey) {
        Project p = projectService.findByKey(projectKey);
        if (!projectService.hasProjectManageAccess(p))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        return workflowRepo.findByProject(p).stream().peek(w -> w.setEditable(true)).collect(Collectors.toList());
    }

    public Workflow update(Long workflowID, String name, String desc, Workflow uw) {
        Optional<Workflow> workflow = findWorkflow(workflowID);
        if (!hasWorkflowManageAccess(workflow.orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND))))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Workflow w = workflow.get();
        w.setName(name);
        w.setDescription(desc);
        w.setDefaultStep(uw.getDefaultStep());
        Set<ConstraintViolation<Workflow>> result = validator.validate(w);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        w.setEditable(true);
        return workflowRepo.save(w);
    }

    public Set<WorkflowStep> getWorkflowSteps(Workflow workflow) {
        return workflowStepRepo.findByWorkflow(workflow).stream().sorted(Comparator.comparing(w -> w.getIssueStatus().getName())).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<WorkflowStep> getAllWorkflowSteps() {
        return workflowStepRepo.findAll().stream().sorted(Comparator.comparing(w -> w.getIssueStatus().getName())).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<WorkflowStep> getAllWorkflowStepsByProject(Project project) {
        return workflowStepRepo.findByWorkflowProject(project).stream().sorted(Comparator.comparing(w -> w.getIssueStatus().getName())).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<WorkflowTransition> getWorkflowTransitions(Workflow workflow) {
        return workflowTransitionRepo.findByWorkflow(workflow);
    }

    public Set<WorkflowTransition> getPossibleTransitions(Workflow workflow, WorkflowStep currentStep) {
        return getWorkflowTransitions(findWorkflow(workflow.getId()).get()).stream().filter(t -> t.isFromAll() || t.getFromStep().equals(currentStep)).collect(Collectors.toSet());
    }

    private WorkflowTransition getWorkflowTransition(Long workflowTransitionID) {
        Optional<WorkflowTransition> workflowTransition = workflowTransitionRepo.findById(workflowTransitionID);
        if (workflowTransition.isEmpty())
            throw new JDException("", ErrorCode.WORKFLOW_TRANSITION_NOT_FOUND, HttpStatus.NOT_FOUND);
        return workflowTransition.get();
    }

    public WorkflowStep addStep(Long workflowID, WorkflowStep step) {
        Workflow workflow = findWorkflow(workflowID).orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!hasWorkflowManageAccess(workflow))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<WorkflowStep> steps = getWorkflowSteps(workflow);
        if (steps.stream().anyMatch(s -> s.getIssueStatus().getName().equalsIgnoreCase(step.getIssueStatus().getName())))
            throw new JDException("Step already exists in workflow", ErrorCode.DUPLICATE, HttpStatus.PRECONDITION_FAILED);
        WorkflowStep s = new WorkflowStep(step.getIssueStatus(), workflow);
        s.setIssueStatus(issueTypeService.saveStatus(step.getIssueStatus()));
        steps.add(s);
        s = workflowStepRepo.save(s);
        return s;
    }

    public WorkflowStep updateStep(Long workflowID, WorkflowStep stp) {
        Workflow workflow = findWorkflow(workflowID).orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!hasWorkflowManageAccess(workflow))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<WorkflowStep> workflowStep = workflowStepRepo.findById(stp.getId());
        if (workflowStep.isEmpty())
            throw new JDException("", ErrorCode.WORKFLOW_STEP_NOT_FOUND, HttpStatus.NOT_FOUND);
        WorkflowStep step = workflowStep.get();
        workflowStepRepo.save(step);
        return step;
    }

    public boolean removeStep(Long workflowID, Long stepID) {
        Workflow workflow = findWorkflow(workflowID).orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!hasWorkflowManageAccess(workflow))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<WorkflowStep> steps = getWorkflowSteps(workflow);
        if (steps.stream().noneMatch(s -> s.getId().equals(stepID)))
            throw new JDException("", ErrorCode.WORKFLOW_STEP_NOT_FOUND, HttpStatus.NOT_FOUND);
        WorkflowStep step = steps.stream().filter(s -> s.getId().equals(stepID)).toList().get(0);
        if (step.getId().equals(workflow.getDefaultStep().getId()))
            throw new JDException("", ErrorCode.CANNOT_EDIT_DELETE_DEFAULT_STEP, HttpStatus.PRECONDITION_FAILED);
        if (!issueRepo.findByCurrentStep(step).isEmpty())
            throw new JDException("", ErrorCode.ACTIVE_STEP, HttpStatus.PRECONDITION_FAILED);
        //Remove associated transactions
        getWorkflowTransitions(workflow).stream().filter(t -> (t.getFromStep() != null && t.getFromStep().equals(step)) || t.getToStep().equals(step)).forEach(t -> workflowTransitionRepo.delete(t));
        workflowStepRepo.delete(step);
        return true;
    }

    public WorkflowTransition addTransition(Long workflowID, String name, Long fromStepID, Long toStepID) {
        Workflow workflow = findWorkflow(workflowID).orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND));
        if (fromStepID.equals(toStepID))
            throw new JDException("", ErrorCode.DUPLICATE, HttpStatus.PRECONDITION_FAILED);
        if (!hasWorkflowManageAccess(workflow))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Optional<WorkflowStep> fromStep = Optional.empty();
        if (fromStepID > 0) {
            fromStep = Optional.of(workflowStepRepo.findById(fromStepID))
                    .orElseThrow(() -> new JDException("", ErrorCode.WORKFLOW_STEP_NOT_FOUND, HttpStatus.NOT_FOUND));
        }
        Optional<WorkflowStep> toStep = Optional.of(workflowStepRepo.findById(toStepID))
                .orElseThrow(() -> new JDException("", ErrorCode.WORKFLOW_STEP_NOT_FOUND, HttpStatus.NOT_FOUND));
        Set<WorkflowTransition> workflowTransitions = getWorkflowTransitions(workflow);
//        if (workflowTransitions.contains(fromStep.get()) || workflowTransitions.contains(toStep.get()))
//            throw new JDException("", ErrorCode.DUPLICATE, HttpStatus.PRECONDITION_FAILED);
        Set<WorkflowTransition> transitions = getWorkflowTransitions(workflow);
        Optional<WorkflowStep> finalFromStep = fromStep;
        if (transitions.stream().filter(t -> t.getFromStep() != null).anyMatch(
                t -> finalFromStep.map(workflowStep -> t.getFromStep().equals(workflowStep) && t.getToStep().equals(toStep.get()))
                        .orElseGet(() -> t.isFromAll() && t.getToStep().equals(toStep.get()))
        )) {
            throw new JDException("", ErrorCode.DUPLICATE, HttpStatus.PRECONDITION_FAILED);
        }
        WorkflowTransition t = new WorkflowTransition(name, fromStep.orElse(null), toStep.get(), workflow);
        if (fromStepID <= 0)
            t.setFromAll(true);
        workflowTransitionRepo.save(t);
        return t;
    }

    public WorkflowTransition updateTransition(Long workflowID, Long workflowTransitionID, String name) {
        Workflow workflow = findWorkflow(workflowID).orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!hasWorkflowManageAccess(workflow))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        WorkflowTransition transition = getWorkflowTransition(workflowTransitionID);
        if (transition.isInitial())
            throw new JDException("", ErrorCode.CANNOT_EDIT_DELETE_DEFAULT_STEP, HttpStatus.PRECONDITION_FAILED);
        transition.setName(name);
        workflowTransitionRepo.save(transition);
        return transition;
    }

    public boolean removeTransition(Long workflowID, Long transitionID) {
        Workflow workflow = findWorkflow(workflowID).orElseThrow(() -> new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!hasWorkflowManageAccess(workflow))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<WorkflowTransition> transitions = getWorkflowTransitions(workflow);
        if (transitions.stream().noneMatch(s -> s.getId().equals(transitionID)))
            throw new JDException("", ErrorCode.WORKFLOW_TRANSITION_NOT_FOUND, HttpStatus.NOT_FOUND);
        WorkflowTransition transition = transitions.stream().filter(s -> s.getId().equals(transitionID)).collect(Collectors.toList()).get(0);
        if (transition.isInitial())
            throw new JDException("", ErrorCode.CANNOT_EDIT_DELETE_DEFAULT_TRANSITION, HttpStatus.PRECONDITION_FAILED);
        workflowTransitionPropertyRepo.deleteAll(workflowTransitionPropertyRepo.findAllByTransition(transition));
        workflowTransitionRepo.delete(transition);
        return true;
    }

    /* Transition post functions */
    public String getWorkflowTransitionProperties(Long workflowID, Long transitionID) {
        WorkflowTransition t = getWorkflowTransition(transitionID);
        if (!t.getWorkflow().getId().equals(workflowID))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        //return workflowTransitionPropertyRepo.findAllByTransition(t);
        JSONObject resp = new JSONObject();
        resp.put("transition", HelperUtil.squiggly("base", t));
        resp.put("transitionProperties", HelperUtil.squiggly("base", getWorkflowTransitionProperties(t)));
        return resp.toString();
    }

    public Set<WorkflowTransitionProperties> getWorkflowTransitionProperties(WorkflowTransition transition) {
        Set<WorkflowTransitionProperties> wtps = workflowTransitionPropertyRepo.findAllByTransition(transition);
        wtps.forEach(wtp -> {
            StringBuilder sb = new StringBuilder();
            switch (wtp.getSubType()) {
                case CONDITION_CURRENT_USER -> Arrays.stream(wtp.getValue().split(",")).forEach(v -> {
                    switch (v) {
                        case "-assignee-" -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("Current user");
                        }
                        case "-reporter-" -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("Reporter");
                        }
                        case "-lead-" -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("Lead");
                        }
                        default -> {
                            try {
                                Optional<Login> l = loginRepo.findById(Long.parseLong(v));
                                l.ifPresent(ll -> {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(ll.getFullName());
                                });
                            } catch (Exception e) {
                                //skip
                            }
                        }
                    }
                });
                case CONDITION_IS_IN_GROUP -> Arrays.stream(wtp.getValue().split(",")).forEach(v -> {
                    try {
                        Optional<Group> l = groupRepo.findById(Long.parseLong(v));
                        l.ifPresent(ll -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(ll.getName());
                        });
                    } catch (Exception e) {
                        //skip
                    }
                });
                case POST_FUNCTION_ASSIGN_TO_USER -> Arrays.stream(wtp.getValue().split(",")).forEach(v -> {
                    switch (v) {
                        case "-none-" -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("Remove assignee");
                        }
                        case "-reporter-" -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("Reporter");
                        }
                        case "-current-" -> {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("Current user");
                        }
                        default -> {
                            try {
                                Optional<Login> l = loginRepo.findById(Long.parseLong(v));
                                l.ifPresent(ll -> {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(ll.getFullName());
                                });
                            } catch (Exception e) {
                                //skip
                            }
                        }
                    }
                });
                case POST_FUNCTION_UPDATE_FIELD -> {
                    switch (wtp.getKey()) {
                        case "Resolution" -> {
                            if (wtp.getValue().equalsIgnoreCase("-none-"))
                                sb.append("Remove Resolution");
                            else
                                resolutionRepo.findById(Long.parseLong(wtp.getValue())).ifPresent(r -> {
                                    sb.append(r.getName());
                                });
                        }
                        case "Priority" -> {
                            if (wtp.getValue().equalsIgnoreCase("-none-"))
                                sb.append("Remove Priority");
                            else
                                sb.append(wtp.getValue());
                        }
                    }
                }
                default -> sb.append(wtp.getValue());
            }
            wtp.setDisplayValue(sb.toString());
        });
        return wtps;
    }

    public WorkflowTransitionProperties updateWorkflowTransitionProperties(Long workflowID, Long
            transitionID, WorkflowTransitionProperties workflowTransitionProperties) {
        workflowTransitionProperties.setTransition(getWorkflowTransition(transitionID));
        if (null == workflowTransitionProperties.getValue()) {
            workflowTransitionProperties.setValue(workflowTransitionProperties.getKey());
            workflowTransitionProperties.setKey(null);
        }
        if (!workflowTransitionProperties.getTransition().getWorkflow().getId().equals(workflowID))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        Set<ConstraintViolation<WorkflowTransitionProperties>> result = validator.validate(workflowTransitionProperties);
        if (result.size() > 0) {
            List<String> details = new ArrayList<>();
            result.forEach(r -> details.add(r.getMessage()));
            throw new JDException(new JSONArray(details).toString(), ErrorCode.VALIDATION_FAILED, HttpStatus.PRECONDITION_FAILED);
        }
        workflowTransitionPropertyRepo.save(workflowTransitionProperties);
        return workflowTransitionProperties;
    }

    public String getTransitionProperties() {
        JSONArray resp = new JSONArray();
        Arrays.stream(WorkflowTransitionPropertyTypes.values()).map(Enum::name).forEach(t -> {
            JSONObject type = new JSONObject();
            type.put("key", t);
            type.put("values", Arrays.stream(WorkflowTransitionPropertySubTypes.values()).map(Enum::name).filter(s -> s.startsWith(t)).toArray());
            type.put("keyMap", Arrays.stream(WorkflowTransitionPropertySubTypes.values()).filter(s -> s.name().startsWith(t))
                    .collect(Collectors.toMap(v -> v.name(),v -> v.displayName())));
            resp.put(type);
        });
        return resp.toString();
    }

    public Fields getWorkflowTransitionPropertiesFields(Long workflowID, Long
            transitionID, WorkflowTransitionPropertySubTypes subType) {
        Workflow w = findWorkflow(workflowID).get();

        Fields field = null;
        switch (subType) {
            case POST_FUNCTION_ASSIGN_TO_USER -> {
                List<FieldValue> users = new ArrayList<>();
                users.add(new FieldValue("-none-", "--None--"));
                users.add(new FieldValue("-current-", "--Current User--"));
                users.add(new FieldValue("-reporter-", "--Reporter--"));
                users.addAll(projectService.getMembers(w.getProject().getId()).stream().map(m -> new FieldValue(m.getId() + "", m.getFullName())).collect(Collectors.toList()));
                field = new Fields("Assign to user", "select", null, users);
            }
            case POST_FUNCTION_UPDATE_FIELD -> {
                field = new Fields("Update field", "sub-select", null, new ArrayList<>());
                ArrayList<SubFields> subFields = new ArrayList<>();
                subFields.add(new SubFields("Resolution", "select", resolutionRepo.findAll().stream().map(r -> new FieldValue(r.getId() + "", r.getName())).collect(Collectors.toList())));
                subFields.add(new SubFields("Priority", "select", Arrays.stream(Priority.values()).map(s -> new FieldValue(s.name(), s.name())).collect(Collectors.toList())));
                field.setSubFields(subFields);
            }
            case CONDITION_HAS_PERMISSION ->
                    field = new Fields("any of", "select-multiple", null, Arrays.stream(AuthorityCode.values()).map(a -> new FieldValue(a.name(), a.name())).collect(Collectors.toList()));
            case CONDITION_FIELD_REQUIRED -> {
                List<FieldValue> fields = new ArrayList<>();
                //issueService.getCustomFieldsForProject(w.getProject()).stsubTypeKeyream().map(a -> new FieldValue(a.getCustomField().getKey(), a.getCustomField().getName())).collect(Collectors.toList());
                fields.add(new FieldValue("resolution", "Resolution"));
//                fields.add(new FieldValue("priority", "Priority"));
//                fields.add(new FieldValue("version", "Version"));
//                fields.add(new FieldValue("component", "Component"));
                field = new Fields("any of", "select-multiple", null, fields);
            }
            case CONDITION_IS_IN_GROUP ->
                    field = new Fields("any of", "select-multiple", null, groupService.getAllGroupsForProject(w.getProject().getKey()).stream().map(a -> new FieldValue(a.getId() + "", a.getName())).collect(Collectors.toList()));
            case CONDITION_CURRENT_USER -> {
                List<FieldValue> cUser = new ArrayList<>();
                cUser.add(new FieldValue("-assignee-", "--Assignee--"));
                cUser.add(new FieldValue("-reporter-", "--Reporter--"));
                cUser.add(new FieldValue("-lead-", "--Project Lead--"));
                cUser.addAll(projectService.getMembers(w.getProject().getId()).stream().map(m -> new FieldValue(m.getId() + "", m.getFullName())).collect(Collectors.toList()));
                field = new Fields("any of", "select-multiple", null, cUser);
            }
            case CONDITION_CHECKLIST_COMPLETE -> {
                List<FieldValue> cListC = new ArrayList<>();
                cListC.add(new FieldValue("-all-", "All items checked"));
                field = new Fields("Require", "select", null, cListC);
            }
        }
        return field;
    }

    public void removeWorkflowTransitionProperties(Long workflowID, Long transitionID, WorkflowTransitionProperties
            workflowTransitionProperties) {
        WorkflowTransition t = getWorkflowTransition(transitionID);
        if (!t.getWorkflow().getId().equals(workflowID))
            throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        workflowTransitionPropertyRepo.findById(workflowTransitionProperties.getId()).ifPresent(workflowTransitionProperties1 -> workflowTransitionPropertyRepo.delete(workflowTransitionProperties));
    }
}

@Data
@AllArgsConstructor
class Fields {
    String label, type, value;
    List<FieldValue> values;
    List<SubFields> SubFields;

    Fields(String label, String type, String value, List<FieldValue> values) {
        this.label = label;
        this.type = type;
        this.value = value;
        this.values = values;
    }
}

@Data
@AllArgsConstructor
class SubFields {
    String label, type;
    List<FieldValue> values;
}

@Data
@AllArgsConstructor
class FieldValue {
    String key, value;
}