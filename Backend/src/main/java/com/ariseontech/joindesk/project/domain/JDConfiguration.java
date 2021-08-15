package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class JDConfiguration extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private ConfigurationService.JDCONFIG key;

    private String stringValue;

    private Long longValue;

    private boolean booleanValue;

    public JDConfiguration(ConfigurationService.JDCONFIG key) {
        this.key = key;
    }

    public JDConfiguration(ConfigurationService.JDCONFIG key, String stringValue) {
        this.key = key;
        this.stringValue = stringValue;
    }

    public JDConfiguration(ConfigurationService.JDCONFIG key, Long longValue) {
        this.key = key;
        this.longValue = longValue;
    }

    public JDConfiguration(ConfigurationService.JDCONFIG key, boolean booleanValue) {
        this.key = key;
        this.booleanValue = booleanValue;
    }
}
