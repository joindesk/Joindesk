package com.ariseontech.joindesk.page.domain;

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
@EqualsAndHashCode(callSuper = false, exclude = "page")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "page_revision")
public class PageRevision extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "page")
    @PropertyView("page_detail_never")
    private Page page;

    private String title;

    private Long version;

    @Column(length = 10485760)
    @PropertyView("page_detail")
    private String content;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "updated_login")
    private Login updatedLogin;

    public PageRevision(@NotNull Page page, Long version, @NotEmpty String content, @NotNull Login updatedLogin) {
        this.page = page;
        this.version = version;
        this.content = content;
        this.updatedLogin = updatedLogin;
    }
}
