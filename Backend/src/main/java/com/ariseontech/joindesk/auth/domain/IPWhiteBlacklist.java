package com.ariseontech.joindesk.auth.domain;

import com.ariseontech.joindesk.AuditModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class IPWhiteBlacklist extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty(message = "cidr is required")
    private String cidr;

    @NotEmpty(message = "Description is required")
    private String description;

    private boolean enabled = false;

    private boolean apiOnly = false;

    private boolean whiteList = false;
}
