package com.ariseontech.joindesk.webhook.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebHookLogHeaders {
    private Long id;
    private String type, key, value;
}
