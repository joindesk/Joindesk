package com.ariseontech.joindesk.event.domain;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.issues.domain.Issue;
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
public class IssueEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    IssueEventType eventType;

    @NotNull(message = "Issue is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "issue_id")
    Issue issue;

    String field, oldValue, newValue;

    @NotNull(message = "by is required")
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "by")
    Login by;
    
    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "mention")
    Login mention;

    public IssueEvent(@NotNull IssueEventType eventType, @NotNull(message = "Issue is required") Issue issue, String field, String oldValue, String newValue, @NotNull(message = "by is required") Login by) {
        this.eventType = eventType;
        this.issue = issue;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.by = by;
    }

	public IssueEvent(@NotNull IssueEventType eventType, @NotNull(message = "Issue is required") Issue issue,
			String field, String oldValue, String newValue, @NotNull(message = "by is required") Login by,
			Login mention) {
		super();
		this.eventType = eventType;
		this.issue = issue;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.by = by;
		this.mention = mention;
	}
    
    
}
