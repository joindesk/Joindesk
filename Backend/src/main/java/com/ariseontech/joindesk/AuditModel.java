package com.ariseontech.joindesk;

import com.github.bohnman.squiggly.view.PropertyView;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
public class AuditModel implements Serializable {
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false, updatable = false)
    @CreatedDate
    @PropertyView("audit_details")
    protected Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated", nullable = false)
    @LastModifiedDate
    @PropertyView("audit_details")
    protected Date updated;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 200, updatable = false)
    @PropertyView("audit_details")
    protected String createdBy = "";

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 200)
    @PropertyView("audit_details")
    protected String updatedBy = "";

}
