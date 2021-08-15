package com.ariseontech.joindesk.board.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.issues.domain.IssueFilter;
import com.ariseontech.joindesk.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Board extends AuditModel implements Serializable {

    private static final long serialVersionUID = 229424520898L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    @Column(columnDefinition = "boolean default false")
    private boolean active = false;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "filter")
    private IssueFilter filter;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "project")
    @JsonIgnore
    private Project project;

    @Transient
    private List<Lane> lanes;

}
