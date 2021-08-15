package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "JD_COMP_IDX", columnList = "project")
})
public class Component extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2)
    private String name;

    @NotNull(message = "Project is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @PropertyView("component_detail")
    private Project project;
}
