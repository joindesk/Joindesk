package com.ariseontech.joindesk.auth.domain;

import com.ariseontech.joindesk.auth.util.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

@Entity
@Table(name = "jd_user", indexes = {
        @Index(name = "JD_USR_E_IDX", columnList = "email"),
        @Index(name = "JD_USR_T_IDX", columnList = "token")
})
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Login implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    @JsonView(View.Public.class)
    private Long id;

    @NotEmpty
    @Column(unique = true)
    @JsonView(View.Public.class)
    private String userName;

    @NotEmpty
    @Size(min = 4)
    @JsonView(View.Public.class)
    private String fullName;

    @NotEmpty
    @Email
    @Column(unique = true)
    @JsonView(View.Details.class)
    private String email;

    /*@Email
    @JsonView(View.Details.class)
    private String recoveryEmail;*/

    @JsonView(View.Admin.class)
    private boolean superAdmin;

    @Size(min = 8, max = 100)
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    @SerializedName("password")
    private String password;

    @JsonIgnore
    private String mfaKey;

    @Column(unique = true)
    @JsonIgnore
    private String apiToken;

    @JsonView(View.Details.class)
    @CreationTimestamp
    private Date createdAt;

    @JsonView(View.Details.class)
    @UpdateTimestamp
    private Date updatedAt;

    @JsonView(View.Details.class)
    private String language;

    @JsonView(View.Details.class)
    private TimeZone timezone;

    @JsonView(View.Public.class)
    private String pic;

    @Column(columnDefinition = "boolean default false")
    @JsonView(View.Public.class)
    private boolean active, locked, mfaEnabled, pendingActivation;

    @Enumerated(EnumType.STRING)
    @JsonView(View.Public.class)
    private PreferredAuthTypes preferredAuth = PreferredAuthTypes.EMAIL;

    @JsonView(View.Details.class)
    private String lockReason;

    @Column(columnDefinition = "boolean default true")
    @JsonView(View.Details.class)
    private boolean emailNotification = true;

    @Column(columnDefinition = "boolean default true")
    @JsonView(View.Details.class)
    private boolean slackNotification;

    @Column(columnDefinition = "boolean default false")
    @JsonView(View.Details.class)
    private boolean slackEnabled, apiEnabled;

    @JsonView(View.Admin.class)
    @Column(unique = true)
    private String slackID;

    @JsonIgnore
    private String slackToken;

    @JsonIgnore
    private String token;

    @JsonIgnore
    private LocalDateTime tokenExpiry;

    @Transient
    @JsonView(View.Details.class)
    private boolean editable = false;

    @Transient
    @JsonView(View.Details.class)
    private String qrUrl;

    @Transient
    @JsonView(View.Details.class)
    private boolean slackAvailable = false;

    public Login() {
    }

    public Login(String fullName, String email, String userName) {
        this.userName = userName;
        this.fullName = fullName;
        this.email = email;
        this.active = true;
        this.createdAt = new Date();
        this.language = "EN";
        this.timezone = TimeZone.getTimeZone("Asia/Kolkata");
    }

    public Login(Long id, String fullName, String email, String userName) {
        this.id = id;
        this.userName = userName;
        this.fullName = fullName;
        this.email = email;
        this.active = true;
        this.createdAt = new Date();
        this.language = "EN";
        this.timezone = TimeZone.getTimeZone("Asia/Kolkata");
    }
}