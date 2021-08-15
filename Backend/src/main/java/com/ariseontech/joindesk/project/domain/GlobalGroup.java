package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.Login;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "jd_global_group")
@NoArgsConstructor
public class GlobalGroup extends AuditModel {

    @ElementCollection(targetClass = AuthorityCode.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "global_role_authorities")
    Set<AuthorityCode> authorityCodes;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 4)
    private String name;

    @ManyToMany(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinTable(name = "global_group_membership")
    private Set<Login> users;

    public GlobalGroup(String name) {
        this.name = name;
        this.users = new HashSet<>();
        this.authorityCodes = EnumSet.noneOf(AuthorityCode.class);
    }
}
