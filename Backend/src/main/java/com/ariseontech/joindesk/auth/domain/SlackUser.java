package com.ariseontech.joindesk.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlackUser {
    String id;
    String name;
    String email;
}
