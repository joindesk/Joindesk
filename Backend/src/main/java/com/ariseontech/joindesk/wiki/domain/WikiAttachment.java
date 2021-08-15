package com.ariseontech.joindesk.wiki.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "wiki")
@ToString(exclude = "wiki")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes = {
        @Index(name = "JD_WIKI_ATTACH_IDX", columnList = "wiki")
})
public class WikiAttachment extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "wiki")
    @PropertyView("wiki_detail_never")
    private Wiki wiki;

    private String name, originalName, thumbnail, type;

    private Long size;

    @Transient
    @PropertyView("wiki_detail")
    private boolean deletable = false;

    @Transient
    private boolean previewable = false;

    @Transient
    private String location;
}
