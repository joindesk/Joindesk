package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Table(name = "jd_group", indexes = {
        @Index(name = "JD_GRP_IDX", columnList = "project")
})
public class Group extends AuditModel {

    @ElementCollection(targetClass = AuthorityCode.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "role_authorities")
    Set<AuthorityCode> authorityCodes;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @NotBlank(message = "Name is required")
    @Size(min = 4)
    private String name;

    @NotNull(message = "Project is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "project")
    private Project project;

    @Column(columnDefinition = "boolean default false")
    private boolean allUsers;

    @ManyToMany(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinTable(name = "group_membership")
    private Set<Login> users;

    @Transient
    private boolean editable = false;

    public Group(String name) {
        this.name = name;
        this.users = new HashSet<>();
        this.authorityCodes = EnumSet.noneOf(AuthorityCode.class);
    }
}
