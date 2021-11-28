package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Component;
import com.ariseontech.joindesk.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.github.bohnman.squiggly.view.PropertyView;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.search.PostgreSQLTSVectorType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

@Entity
@NoArgsConstructor
@Data
@ToString(exclude = {"watchers"})
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
@TypeDef(
        name = "tsvector",
        typeClass = PostgreSQLTSVectorType.class
)
@Table(indexes = {
        @Index(name = "JD_ISSUE_IDX", columnList = "project,key")
})
public class Issue extends AuditModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @NotNull
    private Long key;

    @Transient
    private String keyPair;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "issue_type")
    private IssueType issueType;

    @NotEmpty
    private String summary;

    @Column(length = 10485760)
    private String description;

    @Transient
    private String renderedDescription;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "project")
    private Project project;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter")
    private Login reporter;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee")
    private Login assignee;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "current_step")
    private WorkflowStep currentStep;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "resolution")
    private Resolution resolution;

    private int votes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate resolvedDate;

    private long timeOriginalEstimate;

    @Transient
    private String estimateString, timeSpentString;

    private long timeSpent;

    @Transient
    @PropertyView("issue_detail")
    private Set<WorkflowTransition> possibleTransitions;

    @Transient
    @PropertyView("issue_detail")
    private Set<Attachment> attachments;

    @Transient
    @PropertyView("issue_detail")
    private Set<Comment> comments;

    @Transient
    @PropertyView("issue_detail")
    private Set<Watchers> watchers;

    @PropertyView("issue_detail")
    @Transient
    private Set<IssueCustomField> customFields;

    @Transient
    @PropertyView("issue_detail")
    private Map<String, Boolean> permissions;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinTable(name = "issue_version", joinColumns = {@JoinColumn(name = "id")}, inverseJoinColumns = {@JoinColumn(name = "vid")})
    private Set<Version> versions = new HashSet<>();

    @Transient
    @PropertyView("issue_detail")
    private Set<Version> possibleVersions;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "issue_components", joinColumns = {@JoinColumn(name = "id")}, inverseJoinColumns = {@JoinColumn(name = "vid")})
    private List<Component> components = new ArrayList<>();

    @Transient
    @PropertyView("issue_detail")
    private Set<Component> possibleComponents;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "issue_labels", joinColumns = {@JoinColumn(name = "id")}, inverseJoinColumns = {@JoinColumn(name = "vid")})
    private List<Label> labels = new ArrayList<>();

    @Transient
    private String updateField;

    @Transient
    private Date prevUpdated;

    @Transient
    private Long branchCount, commitCount;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private String data;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private String customData;

    @Type(type = "tsvector")
    @Column(columnDefinition = "tsvector")
    private String content_vector;

    public Issue(Long id) {
        this.id = id;
    }

    public Issue(Long id, @NotNull Project project) {
        this.id = id;
        this.project = project;
    }

    public Issue(@NotNull IssueType issueType, @NotEmpty String summary, String description, @NotNull Project project, @NotNull Login reporter, @NotNull Priority priority) {
        this.issueType = issueType;
        this.summary = summary;
        this.description = description;
        this.project = project;

        this.reporter = reporter;
        if (null == priority)
            this.priority = Priority.Normal;
        else
            this.priority = priority;
        permissions = new HashMap<>();
    }

    public String getKeyPair() {
        return project.getKey() + "-" + key;
    }
}
