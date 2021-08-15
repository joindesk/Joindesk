package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.issues.domain.IssueType;
import com.github.bohnman.squiggly.view.PropertyView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CustomField extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2)
    private String name;

    @NotBlank(message = "Key is required")
    @Size(min = 2)
    private String key;

    @Enumerated(EnumType.STRING)
    private CustomFieldType type;

    private boolean multiple, showOnCreate, required;

    private String value, validation, defaultValue, helpText;

    @NotNull(message = "Project is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project")
    @PropertyView("customfield_detail")
    private Project project;

    @ManyToMany(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinTable(name = "issuetype_customfield", joinColumns = {@JoinColumn(name = "id")}, inverseJoinColumns = {@JoinColumn(name = "vid")})
    private List<IssueType> issueTypes = new ArrayList<>();

}
