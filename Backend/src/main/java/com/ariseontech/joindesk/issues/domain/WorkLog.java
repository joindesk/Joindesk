package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Table(indexes = {
        @Index(name = "JD_WLOG_IDX", columnList = "issue")
})
public class WorkLog extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Timestamp workFrom;

    @Transient
    private Timestamp from, to;

    private long workMinutes;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "by")
    private Login by;

    @Transient
    private String work;

    private String workDescription;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "issue")
    private Issue issue;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @JsonIgnore
    private Project project;

    @Transient
    private boolean editable = false;

}
