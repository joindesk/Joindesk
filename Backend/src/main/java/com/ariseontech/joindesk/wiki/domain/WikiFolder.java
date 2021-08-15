package com.ariseontech.joindesk.wiki.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "project")
@ToString(exclude = "project")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes = {
        @Index(name = "JD_WIKI_FOL_IDX", columnList = "project")
})
public class WikiFolder extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty(message = "Title is required")
    private String title;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @PropertyView("wiki_detail_never")
    private Project project;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_login")
    private Login createdLogin;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "last_updated_login")
    private Login lastUpdatedLogin;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    @Column(columnDefinition = "boolean default false")
    private boolean hasChildFolders;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent")
    private WikiFolder parent;

    @Column(columnDefinition = "boolean default false")
    private boolean projectDefault;

    @Transient
    @PropertyView("wiki_detail")
    private boolean editable = false;

    @Transient
    @PropertyView("wiki_detail")
    private boolean deletable = false;
}
