package com.ariseontech.joindesk.board.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.IssueStatus;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Lane extends AuditModel implements Serializable {

    private static final long serialVersionUID = 3453265934688L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    private int laneOrder;

    @ManyToMany(cascade = CascadeType.DETACH)
    @JoinTable(name = "lane_status", joinColumns = {@JoinColumn(name = "id")}, inverseJoinColumns = {@JoinColumn(name = "vid")})
    private List<IssueStatus> statuses = new ArrayList<>();

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "board")
    @PropertyView("lane_detail")
    private Board board;

    @Transient
    private Set<IssueStatus> possibleStatuses;

    @Transient
    private Set<Issue> issues;
}
