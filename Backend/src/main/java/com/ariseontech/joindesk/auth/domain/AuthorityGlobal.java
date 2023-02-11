package com.ariseontech.joindesk.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Data
@Table(indexes = {
        @Index(name = "JD_AUTH_G_IDX", columnList = "login")
})
public class AuthorityGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "login")
    private Login login;

    @Enumerated(EnumType.STRING)
    private AuthorityCode authorityCode;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}
