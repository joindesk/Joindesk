package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false, exclude = "issue")
@Table(indexes = {
        @Index(name = "JD_ATTACHM_IDX", columnList = "issue")
})
public class Attachment extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "issue")
    @JsonIgnore
    private Issue issue;

    private String name, originalName, type, thumbnail;

    @Transient
    private boolean previewable = false;

    private Long size;

    @Transient
    private String location;

}
