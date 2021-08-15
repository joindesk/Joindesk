package com.ariseontech.joindesk.issues.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class IssueSearchQuery implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String condition = "and";
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<IssueSearchQueryRule> rules = new ArrayList<>();

    IssueSearchQuery() {
    }

    public IssueSearchQuery(String condition, List<IssueSearchQueryRule> rules) {
        this.condition = condition;
        this.rules = rules;
    }
}
