package com.ariseontech.joindesk.exception;

public enum ErrorCode {

    NOT_FOUND(404, "NOT_FOUND", "Resource not found"),
    USER_NOT_FOUND(701, "USER_NOT_FOUND", "User not found"),
    BAD_REQUEST(400, "BAD_REQUEST", "Bad Request"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "Unauthorized"),
    FORBIDDEN(403, "FORBIDDEN", "Forbidden"),
    VALIDATION_FAILED(602, "VALIDATION_FAILED", "Validation failure"),
    DUPLICATE_PROJECT_KEY(601, "DUPLICATE_PROJECT_KEY", "{project.key.v.exists}"),
    PROJECT_NOT_FOUND(701, "PROJECT_NOT_FOUND", "Project not found"),
    GROUP_NOT_FOUND(801, "GROUP_NOT_FOUND", "Group not found"),
    WORKFLOW_NOT_FOUND(901, "WORKFLOW_NOT_FOUND", "Workflow not found"),
    ACTIVE_WORKFLOW(905, "ACTIVE_WORKFLOW", "Workflow is still active"),
    CANNOT_EDIT_DELETE_DEFAULT_STEP(902, "CANNOT_EDIT_DELETE_DEFAULT_STEP", "Cannot delete default step"),
    WORKFLOW_STEP_NOT_FOUND(903, "WORKFLOW_STEP_NOT_FOUND", "Workflow step not found"),
    ACTIVE_STEP(904, "ACTIVE_STEP", "Workflow step is still active"),
    WORKFLOW_TRANSITION_NOT_FOUND(905, "WORKFLOW_TRANSITION_NOT_FOUND", "Workflow transition not found"),
    TRANSITION_ERROR(906, "TRANSITION_ERROR", "Cannot make a transition"),
    CANNOT_EDIT_DELETE_DEFAULT_TRANSITION(902, "CANNOT_EDIT_DELETE_DEFAULT_TRANSITION", "Cannot delete default transition"),
    DUPLICATE(801, "DUPLICATE", "Duplicate action"),
    ISSUE_TYPE_NOT_FOUND(1001, "ISSUE_TYPE_NOT_FOUND", "Issue type not found"),
    ISSUE_NOT_FOUND(1101, "ISSUE_NOT_FOUND", "Issue not found");

    private int code;
    private String key;
    private String details;

    ErrorCode(int code, String key, String details) {
        this.code = code;
        this.key = key;
        this.details = details;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getDetails() {
        return details;
    }
}
