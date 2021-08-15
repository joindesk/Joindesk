package com.ariseontech.joindesk.webhook.domain;

import com.ariseontech.joindesk.AuditModel;
import com.ariseontech.joindesk.event.domain.JDEventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class WebHookLog extends AuditModel {

    @Enumerated(EnumType.STRING)
    private JDEventType eventTypes;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @NotEmpty
    private String endPoint;
    @NotNull
    private Long webhookId;
    @NotNull
    private Long issueId;
    private int statusCode;
    @Column(length = 10485760)
    private String requestBody, responseBody, headers;
}
