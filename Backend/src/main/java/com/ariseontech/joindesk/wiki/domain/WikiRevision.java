package com.ariseontech.joindesk.wiki.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.auth.domain.Login;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "wiki")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WikiRevision extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "wiki")
    @PropertyView("wiki_detail_never")
    private Wiki wiki;

    private String title;

    private Long version;

    @NotEmpty
    private String filename;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "updated_login")
    private Login updatedLogin;

    @Transient
    private String content;

    public WikiRevision(@NotNull Wiki wiki, Long version, @NotEmpty String filename, @NotNull Login updatedLogin) {
        this.wiki = wiki;
        this.version = version;
        this.filename = filename;
        this.updatedLogin = updatedLogin;
    }
}
