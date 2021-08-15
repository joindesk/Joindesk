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

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Table(indexes = {
        @Index(name = "JD_GITBR_IDX", columnList = "issues")
})
public class GitBranch extends AuditModel implements Serializable {

    private static final long serialVersionUID = 229424520898L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    private String issues, url;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "repository")
    @JsonIgnore
    private Repository repository;

    @Transient
    private String repoName;

    public GitBranch(@NotEmpty String name, String issues) {
        this.name = name;
        this.issues = issues;
    }
}
