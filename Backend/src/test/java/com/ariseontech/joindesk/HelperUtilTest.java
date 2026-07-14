package com.ariseontech.joindesk;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HelperUtilTest {

    @Test
    public void testGetUserMentionMatcherRegex() {
        String regex = HelperUtil.getUserMentionMatcherRegex();
        assertNotNull(regex);
        assertTrue(regex.contains("@"));
    }

    @Test
    public void testGetUniqueID() {
        String uniqueId = HelperUtil.getUniqueID();
        assertNotNull(uniqueId);
        assertTrue(uniqueId.contains("-"));
        // uniqueId should be 6 characters + '-' + timestamp
        String[] parts = uniqueId.split("-");
        assertEquals(2, parts.length);
        assertEquals(6, parts[0].length());
    }

    @Test
    public void testGenerateError() {
        String errorMessage = "Invalid input data";
        JSONObject errorJson = HelperUtil.generateError(errorMessage);
        assertNotNull(errorJson);
        assertFalse(errorJson.getBoolean("success"));
        assertEquals(errorMessage, errorJson.getString("error"));
    }

    @Test
    public void testGenerateCronExpression() {
        String cron = HelperUtil.generateCronExpression("0", "15", "10", "*", "*", "?", "*");
        assertEquals("0 15 10 * * ? *", cron);
    }
}
