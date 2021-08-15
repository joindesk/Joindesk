package com.ariseontech.joindesk.event.domain;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.event.repo.IssueEventRepo;
import com.ariseontech.joindesk.event.service.EmailService;
import com.ariseontech.joindesk.issues.domain.*;
import com.ariseontech.joindesk.issues.repo.*;
import com.ariseontech.joindesk.issues.service.IssueAsyncService;
import com.ariseontech.joindesk.project.domain.SlackChannel;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.ariseontech.joindesk.slack.SlackService;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import lombok.Data;
import lombok.extern.java.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Log
public class IssueEventHandler {
    private final int EMAIL = 0;
    private final int SLACK = 1;
    @Autowired
    private EmailService emailService;
    @Autowired
    private SlackService slackService;
    @Autowired
    private WatchersRepo watchersRepo;
    @Autowired
    private IssueSearchCustomRepo issueSearchCustomRepo;
    @Autowired
    private IssueRepo issueRepo;
    @Autowired
    private IssueViewRepo issueViewRepo;
    @Autowired
    private IssueEventRepo issueEventRepo;
    @Autowired
    private IssueHistoryRepo issueHistoryRepo;
    @Autowired
    private AttachmentRepo attachmentRepo;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private IssueAsyncService issueAsyncService;

    @Scheduled(fixedDelay = 300000)
    protected void handleEvent() {
        List<IssueEventType> itemsSkipBatch = Arrays.asList(IssueEventType.DUE, IssueEventType.VIEW, IssueEventType.MENTION);
        long c = issueEventRepo.count();
        log.info(" -- Issues: " + c);
        if (c > 0) {
            List<IssueEvent> events = issueEventRepo.findAll();
            events.forEach(event -> {
                try {
                    if (event.getEventType().equals(IssueEventType.VIEW)) {
                        Optional.ofNullable(event.getIssue()).ifPresent(issue -> {
                            issueViewRepo.deleteByIssueAndWho(issue, event.getBy());
                            issueViewRepo.save(new IssueView(issue, issue.getProject(), event.getBy(), LocalDateTime.now()));
                        });
                    } else {
                        Set<Watchers> watchers = watchersRepo.findByIssue(event.getIssue());
                        // Lead receives due notifications
                        if (event.getEventType().equals(IssueEventType.DUE) && event.getIssue().getProject().getLead() != null)
                            watchers.add(new Watchers(event.getIssue(), event.getIssue().getProject().getLead()));
                        //If mention send only to one mentioned
                        if (event.getEventType().equals(IssueEventType.MENTION)) {
                        	watchers.clear();
                        	Optional.ofNullable(event.getMention()).ifPresent(m -> 
                        		watchers.add(new Watchers(event.getIssue(), m)));
                        }
                        log.info("Sending event notification " + event.getEventType() + " for issue " + event.getIssue().getKeyPair());
                        SlackChannel channel = event.getIssue().getProject().getSlackChannel();
                        if (channel != null && event.getIssue().getProject().isNotifyViaSlack() && slackService.isSlackEnabled()) {
                            switch (event.getEventType()) {
                                case DUE:
                                case CREATE:
                                    slackService.postBlockMessage(channel.getId(), buildSlackMessage(event));
                                    break;
                                case UPDATE:
                                    if (event.getField().equalsIgnoreCase("status")) // only status changes
                                        slackService.postBlockMessage(channel.getId(), buildSlackMessage(event));
                                    break;
                            }
                        }
                        watchers.stream().filter(w -> {
                        	if (event.getEventType().equals(IssueEventType.MENTION))
                                return w.getWatcher().isActive();
                            if (!event.getEventType().equals(IssueEventType.DUE))
                                return !w.getWatcher().equals(event.getBy()) && w.getWatcher().isActive();
                            else return true;
                        }).forEach(w -> {
                            try {
                                if (w.getWatcher().isEmailNotification()
                                        && itemsSkipBatch.contains(event.getEventType())
                                        && !ObjectUtils.isEmpty(w.getWatcher().getEmail())) {
                                    String message = event.getIssue().getProject().getName() + " / " + event.getIssue().getIssueType().getName() + " / " + getSubject(event) + "\n\n" +
                                            getMessage(event, EMAIL);
                                    message = message + "\n\n\n" + configurationService.getApplicationDomain() + "/issue/"+ event.getIssue().getKeyPair();
                                    emailService.sendTextMail(w.getWatcher().getEmail(), getSubject(event), message);
                                }
                                if (w.getWatcher().isSlackNotification() && !ObjectUtils.isEmpty(w.getWatcher().getSlackID())
                                        && slackService.isSlackEnabled())
                                    slackService.postBlockMessage(w.getWatcher().getSlackID(), buildSlackMessage(event));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    issueEventRepo.delete(event);
                }
            });
            events.stream().filter(e -> !itemsSkipBatch.contains(e.getEventType()))
                    .collect(Collectors.groupingBy(IssueEvent::getIssue)).forEach((key, value) -> {
                Set<Watchers> watchers = watchersRepo.findByIssue(key).stream()
                        .filter(w -> w.getWatcher().isActive() && w.getWatcher().isEmailNotification()
                                && !ObjectUtils.isEmpty(w.getWatcher().getEmail())).collect(Collectors.toSet());
                if (!watchers.isEmpty()) {
                    String subject = getSubject(value.get(0));
                    StringBuffer mb = new StringBuffer();
                    List<LayoutBlock> slackblocks = new ArrayList<>();
                    mb.append(key.getProject().getName()).append(" / ").append(key.getIssueType().getName()).append(" / ").append(subject).append("\n\n");
                    if (value.size() > 1)
                        mb.append("Multiple updates for issue").append("\n\n");
                    value.forEach(ev -> {
                        try {
                            mb.append(getMessage(ev, EMAIL));
                            mb.append("\n\n-------------------------------\n\n");
                        } catch (ParseException parseException) {
                            parseException.printStackTrace();
                        }
                        try {
                            slackblocks.addAll(buildSlackMessage(ev));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    mb.append("\n").append(configurationService.getApplicationDomain() + "/issue/"+ key.getKeyPair());
                    watchers.forEach(w -> {
                        emailService.sendTextMail(w.getWatcher().getEmail(), subject, mb.toString());
                        if (w.getWatcher().isSlackNotification() && !ObjectUtils.isEmpty(w.getWatcher().getSlackID())
                                && slackService.isSlackEnabled()) {
                            try {
                                slackService.postBlockMessage(w.getWatcher().getSlackID(), slackblocks);
                            } catch (IOException | SlackApiException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
    }

    private String getSubject(IssueEvent event) {
        return "(" + event.getIssue().getProject().getKey() + "-" + event.getIssue().getKey() + ") " + event.getIssue().getSummary();
    }

    public void sendMessage(IssueEvent message) {
        if (null != message) {
            issueEventRepo.save(message);
            log.info("Sent message to DB");
        }
    }

    private List<LayoutBlock> buildSlackMessage(IssueEvent event) {
        List<LayoutBlock> message = new ArrayList<>();
        Issue i = event.getIssue();
        String by = event.getBy() != null ? (ObjectUtils.isEmpty(event.getBy().getSlackID()) ? event.getBy().getFullName() : "<@" + event.getBy().getSlackID() + ">") : "SYSTEM";
        String reporter = ObjectUtils.isEmpty(i.getReporter().getSlackID()) ? i.getReporter().getFullName() : "<@" + i.getReporter().getSlackID() + ">";
        String assignee = (i.getAssignee() == null) ? "_unassigned_" : (ObjectUtils.isEmpty(i.getAssignee().getSlackID()) ? i.getReporter().getFullName() : "<@" + i.getAssignee().getSlackID() + ">");
        switch (event.eventType) {
            case CREATE:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _created issue_")
                                .build())
                        .build());
                break;
            case UPDATE:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _updated " + event.getField() + "_")
                                .build())
                        .build());
                message.add(SectionBlock
                        .builder()
                        .fields(Arrays.asList(MarkdownTextObject
                                        .builder()
                                        .text("*Current*\n" + event.getNewValue())
                                        .build(),
                                MarkdownTextObject
                                        .builder()
                                        .text("*Previously*\n" + event.getOldValue())
                                        .build()))
                        .build());
                break;
            case ASSIGN:
            case REASSIGN:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _has assigned ticket to *" + event.getNewValue() + "*" + (!ObjectUtils.isEmpty(event.getOldValue()) ? " (previous: *" + event.getOldValue() + "*)" : "") + "_")
                                .build())
                        .build());
                break;
            case MENTION:
                if (event.field.equalsIgnoreCase("comment"))
                    message.add(SectionBlock
                            .builder()
                            .text(MarkdownTextObject
                                    .builder()
                                    .text(by + " _has mentioned you in a comment_")
                                    .build())
                            .build());
                if (event.field.equalsIgnoreCase("description"))
                    message.add(SectionBlock
                            .builder()
                            .text(MarkdownTextObject
                                    .builder()
                                    .text(by + " _has mentioned you in issue description_")
                                    .build())
                            .build());
                break;
            case ATTACH:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _has attached a file_ " + event.getNewValue())
                                .build())
                        .build());
                break;
            case ATTACH_DELETE:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _has removed a file_ " + event.getOldValue())
                                .build())
                        .build());
                break;
            case COMMENT:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _added a comment_ " + "\n*Comment*:\n" + event.getNewValue())
                                .build())
                        .build());
                break;
            case COMMENT_DELETE:
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(by + " _removed a comment_ " + "\n*Comment*:\n" + event.getNewValue())
                                .build())
                        .build());
                break;
            case DUE:
                String msg = "";
                if (event.getIssue().getDueDate().getDayOfYear() == LocalDate.now().getDayOfYear())
                    msg = "issue is due today";
                else
                    msg = "issue was due on " + event.getIssue().getDueDate().format(DateTimeFormatter.ofPattern("YYYY/MM/dd"))
                            + " (" + ChronoUnit.DAYS.between(event.getIssue().getDueDate(), LocalDate.now()) + " days ago)";
                message.add(SectionBlock
                        .builder()
                        .text(MarkdownTextObject
                                .builder()
                                .text(msg)
                                .build())
                        .build());
                break;
        }
        message.add(DividerBlock.builder().build());
        message.add(SectionBlock.builder()
                .text(MarkdownTextObject
                        .builder()
                        .text("*[" + i.getKeyPair() + "] " + i.getSummary() + "*")
                        .build())
                .build());
        message.add(SectionBlock.builder().fields(Arrays.asList(
                MarkdownTextObject
                        .builder()
                        .text("*Status*\n" + i.getCurrentStep().getIssueStatus().getName())
                        .build(),
                MarkdownTextObject
                        .builder()
                        .text("*Assignee*\n" + assignee)
                        .build(),
                MarkdownTextObject
                        .builder()
                        .text("*Priority*\n" + i.getPriority())
                        .build(),
                MarkdownTextObject
                        .builder()
                        .text("*Reporter*\n" + reporter)
                        .build()
        )).build());
        message.add(ActionsBlock.builder().elements(Arrays.asList(
                ButtonElement.builder().value("view").actionId("view").url(configurationService.getApplicationDomain() + "issue/" + i.getKeyPair()).text(PlainTextObject.builder().text("View").build()).build(),
                ButtonElement.builder().value("comment").actionId("comment").value(i.getKeyPair()).text(PlainTextObject.builder().text("Comment").build()).build()
        )).build());
        return message;
    }

    private String getMessage(IssueEvent event, int type) throws ParseException {
        RuntimeServices rs = RuntimeSingleton.getRuntimeServices();
        StringBuilder message = new StringBuilder();

        switch (event.eventType) {
            case CREATE:
                message.append("Issue Created\n\nSummary: ${issuesummary}");
                message.append("\nBy: ${by}");
                break;
            case UPDATE:
                message.append("Issue Updated\n\n").append(event.field).append(": ").append(event.getNewValue());
                if (null != event.getOldValue() && !ObjectUtils.isEmpty(event.getOldValue()))
                    message.append(" (was ").append(event.getOldValue()).append(")");
                message.append("\nBy: ${by}");
                break;
            case ASSIGN:
                message.append("Issue was assigned to ").append(event.getNewValue());
                if (null != event.getOldValue() && !ObjectUtils.isEmpty(event.getOldValue()))
                    message.append(" (was ").append(event.getOldValue()).append(")");
                message.append("\nBy: ${by}");
                break;
            case REASSIGN:
                message.append("Issue was reassigned to ").append(event.getNewValue()).append(" (was ").append(event.getOldValue()).append(")");
                message.append("\nBy: ${by}");
                break;
            case ATTACH:
                message.append("New Attachment\n\n Attachment:").append(event.getNewValue());
                message.append("\nBy: ${by}");
                break;
            case ATTACH_DELETE:
                message.append("Attachment Removed\n\n Attachment:").append(event.getOldValue());
                message.append("\nBy: ${by}");
                break;
            case COMMENT:
                message.append("${by} commented on issue \n\n").append(event.getNewValue());
                break;
            case MENTION:
                if (event.field.equalsIgnoreCase("comment"))
                    message.append("You were mentioned in comment by ${by} \n\n").append(event.getNewValue());
                if (event.field.equalsIgnoreCase("description"))
                    message.append("You were mentioned in description by ${by} \n\n").append(event.getNewValue());
                break;
            case COMMENT_DELETE:
                message.append("${by} deleted comment \n\n").append(event.getOldValue());
                break;
            case DUE:
                if (event.getIssue().getDueDate().getDayOfYear() == LocalDate.now().getDayOfYear())
                    message.append("${issuetype} is due today");
                else
                    message.append("${issuetype} was due on ").append(event.getIssue().getDueDate().format(DateTimeFormatter.ofPattern("YYYY/MM/dd")))
                            .append(" (").append(ChronoUnit.DAYS.between(event.getIssue().getDueDate(), LocalDate.now())).append(" days ago)");
                break;
        }
        if (type == SLACK)
            message.append("\n");
        
        SimpleNode sn = rs.parse(new StringReader(message.toString()), "Template");

        Template t = new Template();
        t.setRuntimeServices(rs);
        t.setData(sn);
        t.initDocument();

        VelocityContext vc = new VelocityContext();
        vc.put("issuesummary", event.getIssue().getSummary());
        vc.put("issueproject", event.getIssue().getProject().getName());
        vc.put("issuetype", event.getIssue().getIssueType().getName());
        vc.put("issueprojectkey", event.getIssue().getProject().getKey());
        vc.put("issuekey", event.getIssue().getKey());
        vc.put("by", event.getBy() != null ? event.getBy().getFullName() : "SYSTEM");

        StringWriter sw = new StringWriter();
        t.merge(vc, sw);

        return sw.toString();
    }

    public void LogHistory(IssueHistory issueHistory) {
        issueHistoryRepo.save(issueHistory);
    }

    public void updateIssue(Issue issue) {
        issueAsyncService.updateIssue(issue);
    }

    public void deleteByIssue(Issue issue) {
        issueEventRepo.deleteByIssue(issue);
    }

    public String jsonData(Issue issue) {
        JSONObject jsonData = new JSONObject();
        jsonData.put("type", issue.getIssueType().getId());
        jsonData.put("resolution", issue.getResolution() != null ? issue.getResolution().getName() : "unresolved");
        jsonData.put("version", issue.getVersions().stream().map(Version::getId).collect(Collectors.toList()));
        jsonData.put("label", issue.getLabels().stream().map(Label::getId).collect(Collectors.toList()));
        jsonData.put("component", issue.getComponents().stream().map(com.ariseontech.joindesk.project.domain.Component::getId).collect(Collectors.toList()));
        JSONObject countsData = new JSONObject();
        countsData.put("label", issue.getLabels().size());
        countsData.put("component", issue.getComponents().size());
        countsData.put("attachments", attachmentRepo.countByIssue(issue));
        jsonData.put("count", countsData);
        return jsonData.toString();
    }

    public void reindexAll() {
        issueSearchCustomRepo.findUnIndexed().forEach(i -> {
            Issue issue = (Issue) i;
            issueSearchCustomRepo.update(jsonData(issue), issue);
        });
    }

    public void addWatcher(Issue issue, Login watcher) {
        if (null != watcher && watchersRepo.findByIssueAndWatcher(issue, watcher).isEmpty())
            watchersRepo.save(new Watchers(issue, watcher));
    }

    @Data
    static class IssueEventMap {
        Issue issue;
        List<IssueEvent> events;

        public IssueEventMap() {
            this.events = new ArrayList<>();
        }
    }
}