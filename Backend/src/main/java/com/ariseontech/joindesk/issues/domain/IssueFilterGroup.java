package com.ariseontech.joindesk.issues.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class IssueFilterGroup {
    private String field, label, type, value;
    private Date fromDate, toDate;
    private List<String> values, operators;
    private List<IssueFilterOptions> options;

    public IssueFilterGroup(String field, String label, String type) {
        this.field = field;
        this.label = label;
        this.type = type;
        switch (type) {
            case "multiselect":
                this.operators = new ArrayList<>();
                this.operators.add("IN");
                this.operators.add("NOT IN");
                break;
            case "date":
                this.operators = new ArrayList<>();
                this.operators.add("TODAY");
                this.operators.add("THIS WEEK");
                this.operators.add("THIS MONTH");
                this.operators.add("LAST MONTH");
                this.operators.add("THIS YEAR");
                this.operators.add("LAST YEAR");
                this.operators.add("CUSTOM");
                this.operators.add("ANY");
                break;
        }
        if (this.field.equalsIgnoreCase("due_date"))
            this.operators.add("ALREADY");
        this.options = new ArrayList<>();
    }

    public IssueFilterGroup(String field, String label, String type, String value) {
        this(field, label, type);
        this.value = value;
    }
}
