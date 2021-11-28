package com.ariseontech.joindesk.issues.domain;

public enum WorkflowTransitionPropertySubTypes {
    CONDITION_CURRENT_USER("User is"),
    CONDITION_IS_IN_GROUP("User is in group"),
    CONDITION_HAS_PERMISSION("User has permission"),
    CONDITION_FIELD_REQUIRED("Required check for field"),
    CONDITION_CHECKLIST_COMPLETE("Issue checklist is complete"),
    POST_FUNCTION_ASSIGN_TO_USER("Assign issue to user"),
    POST_FUNCTION_UPDATE_FIELD("Update issue field");

    private String displayName;

    WorkflowTransitionPropertySubTypes(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    // Optionally and/or additionally, toString.
    @Override
    public String toString() {
        return displayName;
    }
}
