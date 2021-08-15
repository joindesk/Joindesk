package com.ariseontech.joindesk.auth.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jd_token")
@EqualsAndHashCode(callSuper = false)
public class Token extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    @Column(length = 5000)
    @JsonIgnore
    private String token;


    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private Login user;

    private Date lastAccess;

    @Column(length = 2000)
    private String deviceInfo;

    private String ip, fp;

    @Transient
    private Map<String, String> device;

    @Transient
    private boolean current;

    @Lob
    @JsonIgnore
    private String authorities;

    @Lob
    @JsonIgnore
    private String globalAuthorities;

}
