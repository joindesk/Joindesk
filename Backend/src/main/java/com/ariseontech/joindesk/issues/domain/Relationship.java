package com.ariseontech.joindesk.issues.domain;

import com.ariseontech.joindesk.AuditModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class Relationship extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotEmpty
    @Column(unique = true, nullable = false)
    private String name;
    @NotEmpty
    @Column(unique = true, nullable = false)
    private String outwardDesc;
    @Column(unique = true)
    private String inwardDesc;

    public Relationship() {
    }

    public Relationship(Long id, @NotEmpty String name, @NotEmpty String outwardDesc, String inwardDesc) {
        this.id = id;
        this.name = name;
        this.outwardDesc = outwardDesc;
        this.inwardDesc = inwardDesc;
    }
}
