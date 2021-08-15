package com.ariseontech.joindesk.git.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.project.domain.Project;
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
        @Index(name = "JD_REPO_IDX", columnList = "project")
})
public class Repository extends AuditModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String name;

    @NotEmpty
    private String repoUrl;

    @Column(unique = true)
    private String uuid;

    @Transient
    private String hookEndpoint;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "project")
    @JsonIgnore
    private Project project;

    private boolean active;

    @Enumerated(EnumType.STRING)
    private GitRepoType repoType;

}
