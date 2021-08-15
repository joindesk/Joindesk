package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.project.domain.Project;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Table(indexes = {
        @Index(name = "JD_VERS_IDX", columnList = "project")
})
public class Version extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String key;

    @NotEmpty
    private String name;

    private String description;

    private Date startDate;

    private Date releaseDate;

    @NotNull(message = "Project is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @PropertyView("version_detail")
    private Project project;
//
//    @PropertyView("version_detail_never")
//    private Issue issue;

    @Column(columnDefinition = "boolean default false")
    private boolean released = false;

    @Transient
    private boolean editable = false;

    @Transient
    private int totalIssues, totalResolved;

}
