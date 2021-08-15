package com.ariseontech.joindesk.page.domain;

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
@EqualsAndHashCode(callSuper = false, exclude = "page")
@ToString(exclude = "page")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes = {
        @Index(name = "JD_PAGE_ATTACH_IDX", columnList = "page")
})
public class PageAttachment extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "page")
    @PropertyView("page_detail_never")
    private Page page;

    private String name, originalName, thumbnail, type;

    private Long size;

    @Transient
    @PropertyView("page_detail")
    private boolean deletable = false;

    @Transient
    private boolean previewable = false;

    @Transient
    private String location;
}
