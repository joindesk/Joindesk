package com.ariseontech.joindesk.project.domain;

import com.ariseontech.joindesk.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "project")
@ToString(exclude = "project")
@Table(indexes = {
        @Index(name = "JD_TIMET_IDX", columnList = "project")
})
public class TimeTracking extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "project", unique = true, nullable = false)
    @JsonIgnore
    private Project project;

    private boolean enabled;

    @Min(1)
    @Max(7)
    private int daysPerWeek;

    @Min(1)
    @Max(24)
    private int hoursPerDay;

    @Min(0)
    @Max(1)
    private int timeFormat;

    public TimeTracking(Project project) {
        this.project = project;
        enabled = false;
        hoursPerDay = 8;
        daysPerWeek = 5;
        timeFormat = 0;
    }
}
