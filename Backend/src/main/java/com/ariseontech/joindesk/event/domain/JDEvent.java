package com.ariseontech.joindesk.event.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
public class JDEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    JDEventType jdEventType;
    Long issueID;
    Long projectID;
    String issueKeyPair;
    String field, oldValue, newValue;

    public JDEvent(@NotNull JDEventType jdEventType, Long issueID, Long projectID, String issueKeyPair, String field, String oldValue, String newValue) {
        this.jdEventType = jdEventType;
        this.issueID = issueID;
        this.projectID = projectID;
        this.issueKeyPair = issueKeyPair;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
