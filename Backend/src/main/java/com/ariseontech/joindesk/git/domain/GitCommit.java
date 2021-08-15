package com.ariseontech.joindesk.git.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Table(indexes = {
        @Index(name = "JD_GITCO_IDX", columnList = "issues")
})
public class GitCommit extends AuditModel implements Serializable {

    private static final long serialVersionUID = 229424520898L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name, commitId;

    private String issues, url, author;

    private LocalDate timestamp;

    private int added, modified, removed;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "repository")
    @JsonIgnore
    private Repository repository;

    @Transient
    private String repoName;
}
