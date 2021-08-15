package com.ariseontech.joindesk.auth.domain;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Entity
@Data
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    @Size(min = 4)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private AuthorityCode authority;

}
