package com.ariseontech.joindesk.slack;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.repo.LoginRepo;
import com.ariseontech.joindesk.auth.service.UserService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import com.ariseontech.joindesk.event.domain.IssueEvent;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import com.ariseontech.joindesk.issues.domain.Comment;
import com.ariseontech.joindesk.issues.service.IssueService;
import com.ariseontech.joindesk.project.service.ConfigurationService;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.bots.BotsInfoRequest;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.dialog.DialogOpenRequest;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.request.team.TeamInfoRequest;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.bots.BotsInfoResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.dialog.DialogOpenResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.methods.response.team.TeamInfoResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.Action;
import com.slack.api.model.Attachment;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.dialog.Dialog;
import com.slack.api.model.dialog.DialogTextAreaElement;
import lombok.extern.java.Log;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log
public class SlackService {
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private UserService userService;
    @Autowired
    private CurrentLogin currentLogin;
    @Value("${slack.client.id}")
    private String slackClientID = "";
    @Value("${slack.client.secret}")
    private String slackClientSecret = "";
    @Autowired
    private IssueService issueService;
    @Autowired
    private LoginRepo loginRepo;

    public boolean isSlackEnabled() {
        return configurationService.getBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED);
    }

    private String getSlackToken() {
        return configurationService.getString(ConfigurationService.JDCONFIG.APP_SLACK_ACCESS_TOKEN);
    }

    public String getBotID() {
        return configurationService.getString(ConfigurationService.JDCONFIG.APP_SLACK_BOT_ID);
    }

    public String getClientID() {
        return slackClientID;
    }

    @CacheEvict(value = "configuration", allEntries = true)
    public void connect(String code) throws IOException, SlackApiException {
        if (slackClientID.isEmpty() || slackClientSecret.isEmpty()) {
            configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED, false);
            return;
        }
        OAuthV2AccessResponse resp = Slack.getInstance().methods().oauthV2Access(OAuthV2AccessRequest.builder().code(code).clientId(slackClientID).clientSecret(slackClientSecret).redirectUri("").build());
        if (!resp.isOk()) slackError(resp.getError());
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED, true);
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_ACCESS_TOKEN, resp.getAccessToken());
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_BOT_ID, resp.getBotUserId());
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_TEAM_ID, resp.getTeam().getId());
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_TEAM_NAME, resp.getTeam().getName());
        getUsers().getMembers().forEach(user -> {
            if (null == loginRepo.findBySlackID(user.getId())) {
                Login login = loginRepo.findByEmailIgnoreCase(user.getProfile().getEmail());
                if (null != login) {
                    login.setSlackEnabled(true);
                    login.setSlackID(user.getId());
                    loginRepo.save(login);
                }
            }
        });
    }

    @CacheEvict(value = "configuration", allEntries = true)
    public void disconnect() {
        configurationService.saveBoolean(ConfigurationService.JDCONFIG.APP_SLACK_ENABLED, false);
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_ACCESS_TOKEN, "");
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_BOT_ID, "");
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_TEAM_ID, "");
        configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_TEAM_NAME, "");
    }

    public boolean validateToken(String token) throws IOException, SlackApiException {
        BotsInfoResponse resp = Slack.getInstance().methods().botsInfo(BotsInfoRequest.builder().token(token).build());
        if (!resp.isOk()) slackError(resp.getError());
        return resp.isOk();
    }

    public UsersListResponse getUsers() throws IOException, SlackApiException {
        if (!isSlackEnabled()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        UsersListResponse resp = Slack.getInstance().methods().usersList(UsersListRequest.builder().token(getSlackToken()).build());
        if (!resp.isOk()) slackError(resp.getError());
        return resp;
    }

    public List<Conversation> getConversations() throws IOException, SlackApiException {
        if (!isSlackEnabled()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        List<ConversationType> types = new ArrayList<>();
        types.add(ConversationType.PRIVATE_CHANNEL);
        types.add(ConversationType.PUBLIC_CHANNEL);
        ConversationsListResponse response = Slack.getInstance().methods().conversationsList(ConversationsListRequest.builder()
                .types(types).token(getSlackToken()).build());
        if (!response.isOk()) slackError(response.getError());
        return response.getChannels().stream().filter(c -> !c.isArchived()).collect(Collectors.toList());
    }

    public void postBlockMessage(String userName, List<LayoutBlock> message) throws IOException, SlackApiException {
        if (!isSlackEnabled()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        ChatPostMessageRequest.ChatPostMessageRequestBuilder builder = ChatPostMessageRequest.builder();
        builder.blocks(message);
        ChatPostMessageResponse resp = Slack.getInstance().methods().chatPostMessage(builder.channel(userName).asUser(true).token(getSlackToken()).build());
        if (!resp.isOk()) slackError(resp.getError());
    }

    public void postMessage(String userName, String message, IssueEvent event) throws IOException, SlackApiException {
        if (!isSlackEnabled()) throw new JDException("", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        ChatPostMessageRequest.ChatPostMessageRequestBuilder builder = ChatPostMessageRequest.builder();
        if (event != null)
            builder.attachments(Collections.singletonList(getSlackAttachments(event, message)));
        else
            builder.text(message);
        ChatPostMessageResponse resp = Slack.getInstance().methods().chatPostMessage(builder.channel(userName).asUser(true).token(getSlackToken()).build());
        if (!resp.isOk()) slackError(resp.getError());
    }

    public Attachment getSlackAttachments(IssueEvent event, String message) {
        String issueKey = event.getIssue().getProject().getKey() + "-" + event.getIssue().getKey();
        String issueLink = configurationService.getApplicationDomain() + "issue/" + issueKey;
        return Attachment.builder().fallback("[" + issueKey + "]" + event.getIssue().getSummary()).text(message).
                titleLink(issueLink).color("#7CD197").title("[" + issueKey + "]" + event.getIssue().getSummary()).
                actions(Collections.singletonList(Action.builder().type(Action.Type.BUTTON).text(issueKey).url(issueLink).build())).build();
    }

    public UsersInfoResponse getUser(String id) throws IOException, SlackApiException {
        UsersInfoResponse resp = Slack.getInstance().methods().usersInfo(UsersInfoRequest.builder().token(getSlackToken()).user(id).build());
        if (!resp.isOk()) slackError(resp.getError());
        return resp;
    }

    public TeamInfoResponse getTeamInfo() throws IOException, SlackApiException {
        TeamInfoResponse resp = Slack.getInstance().methods().teamInfo(TeamInfoRequest.builder().token(getSlackToken()).build());
        if (!resp.isOk()) slackError(resp.getError());
        return resp;
    }

    public BotsInfoResponse getBotInfo() throws IOException, SlackApiException {
        BotsInfoResponse resp = Slack.getInstance().methods().botsInfo(BotsInfoRequest.builder().token(getSlackToken()).build());
        if (!resp.isOk()) slackError(resp.getError());
        return resp;
    }

    private void slackError(String error) {
        System.out.println("Slack Error " + error);
        if (error.equals("invalid_auth") || error.equals("not_authed")) {
            disconnect();
            configurationService.save(ConfigurationService.JDCONFIG.APP_SLACK_FAILURE, error);
        }
        throw new JDException("Slack Error: " + error, ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    private void openSlackDialog(String triggerID, String keyPair) throws IOException, SlackApiException {
        Dialog d = new Dialog("Add comment", "comment",
                Arrays.asList(DialogTextAreaElement.builder().optional(false).label("Message").name("Message").maxLength(2000).build()),
                "Comment", false, keyPair);
        d.setSubmitLabel("Comment");
        DialogOpenResponse resp = Slack.getInstance().methods().dialogOpen(DialogOpenRequest.builder().
                triggerId(triggerID).token(getSlackToken()).dialog(d).build());
        //log.info(resp.toString());
        if (!resp.isOk()) slackError(resp.getError());

    }

    public void handleEvent(String eventType, JSONObject payload) {
        //Event callback
    }

    public void handleAction(JSONObject data) throws IOException, SlackApiException {
        JSONObject resp = new JSONObject();
        resp.put("text", "Unable to handle request");
        resp.put("response_type", "ephemeral");
        resp.put("replace_original", false);
        boolean ack = true;
        switch (data.getString("type")) {
            case "block_actions":
                ack = false;
                String userID = data.getJSONObject("user").getString("id");
                try {
                    userService.getSlackUser(userID);
                } catch (Exception e) {
                    resp.put("text", "User not connected");
                }
                String actionID = data.getJSONArray("actions").getJSONObject(0).getString("action_id");
                switch (actionID) {
                    case "comment":
                        openSlackDialog(data.getString("trigger_id"), data.getJSONArray("actions").getJSONObject(0).getString("value"));
                        break;
                }
                break;
            case "dialog_submission":
                String userID2 = data.getJSONObject("user").getString("id");
                try {
                    Login by = userService.getSlackUser(userID2);
                    String message = data.getJSONObject("submission").getString("Message");
                    String projectKey = data.getString("state").substring(0, data.getString("state").indexOf("-"));
                    String issueKey = data.getString("state").substring(data.getString("state").indexOf("-") + 1);
                    try {
                        currentLogin.setUser(by);
                        issueService.saveComment(new Comment(message), projectKey, Long.parseLong(issueKey));
                        currentLogin.setUser(null);
                        resp.put("text", "Commented added to issue");
                    } catch (Exception e) {
                        resp.put("text", "Unable to add comment");
                    }
                } catch (Exception e) {
                    resp.put("text", "User not connected");
                }

                break;
        }
        if (ack && data.has("response_url"))
            executeHook(resp.toString(), data.getString("response_url"));
    }

    private void executeHook(String requestBody, String hookUrl) throws IOException {
        HttpPost httppost = new HttpPost(hookUrl);
        StringEntity entity = new StringEntity(requestBody);
        httppost.setEntity(entity);
        HttpClients.createDefault().execute(httppost);
    }
}
