package com.ariseontech.joindesk.issues.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class IssueSearchQueryRule implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String field, operator, valueFrom, valueTo;
    private String[] values;
    @Transient
    private List<String> expandedValues = new ArrayList<>();

    public IssueSearchQueryRule(String field, String operator, String[] values) {
        this.field = field;
        this.operator = operator;
        this.values = values;
    }
}
