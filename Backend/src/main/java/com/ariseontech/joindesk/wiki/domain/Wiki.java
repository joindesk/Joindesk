package com.ariseontech.joindesk.wiki.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.bohnman.squiggly.view.PropertyView;
import com.vladmihalcea.hibernate.type.search.PostgreSQLTSVectorType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "project")
@ToString(exclude = "project")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@TypeDef(
        name = "tsvector",
        typeClass = PostgreSQLTSVectorType.class
)
@Table(indexes = {
        @Index(name = "JD_WIKI_IDX", columnList = "project, folder")
})
public class Wiki extends AuditModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    private String title;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @PropertyView("wiki_detail_never")
    private Project project;

    @NotNull(message = "Folder is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "folder")
    private WikiFolder folder;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "created_login")
    private Login createdLogin;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "last_updated_login")
    private Login lastUpdatedLogin;

    @Column(length = 10485760)
    @PropertyView("wiki_detail")
    private String content;

    @Type(type = "tsvector")
    @Column(columnDefinition = "tsvector")
    private String content_vector;

    @Transient
    private List<WikiPath> tree;

    @Transient
    @PropertyView("wiki_detail")
    private boolean editable = false;

    @Transient
    @PropertyView("wiki_detail")
    private boolean deletable = false;
}
