package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.repo.ReportDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "JD_PRJ_KY_IDX", columnList = "key")
})
public class Project extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Project key is required")
    @Size(min = 2, max = 10, message = "Project key should be between 2 to 10")
    private String key;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @PropertyView("project_detail")
    private boolean active;

    @Transient
    @PropertyView("project_detail")
    private TimeTracking timeTracking;

    @NotNull(message = "Lead is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "lead_id")
    private Login lead;

    @Transient
    @PropertyView("project_detail")
    private boolean editable = false;

    @Transient
    @PropertyView("project_detail")
    private List<AuthorityCode> authorities;

    @Transient
    @PropertyView("project_overview")
    @JsonProperty("report")
    private ReportDTO reportDTO;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "slack_channel_id")
    @PropertyView("project_detail")
    private SlackChannel slackChannel;

    @Column(columnDefinition = "boolean default false")
    private boolean notifyViaSlack;

    public Project(@NotBlank @Size(min = 2, max = 6) String key, @NotBlank String name) {
        this.key = key;
        this.name = name;
        this.active = true;
    }
}
