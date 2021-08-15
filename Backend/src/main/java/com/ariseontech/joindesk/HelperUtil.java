package com.ariseontech.joindesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.bohnman.squiggly.Squiggly;
import com.github.bohnman.squiggly.util.SquigglyUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Date;
import java.util.UUID;

@Service
public class HelperUtil {

    private final static String mentionMatchPattern = "(?<=^|(?<=[^a-zA-Z0-9-\\.]))@[A-Za-z0-9-]+(?=[^a-zA-Z0-9-_\\.])";
    @Value("${data-dir}")
    private String dataPath;

    public static String getUserMentionMatcherRegex() {
        return mentionMatchPattern;
    }

    public static String getUniqueID() {
        return UUID.randomUUID().toString().substring(0, 6) + "-" + new Date().getTime();
    }

    public static String generateApiToken(String username) {
        return new BCryptPasswordEncoder().encode(username + new Date().getTime());
    }

    public static JSONObject generateError(String error) {
        return new JSONObject().put("success", false).put("error", error);
    }

    public static String squiggly(String filter, Object object) {
        ObjectMapper mapper = Squiggly.init(new ObjectMapper(), filter);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return SquigglyUtils.stringify(mapper, object);
    }

    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }

    /**
     * Generate a CRON expression is a string comprising 6 or 7 fields separated by white space.
     *
     * @param seconds    mandatory = yes. allowed values = {@code  0-59    * / , -}
     * @param minutes    mandatory = yes. allowed values = {@code  0-59    * / , -}
     * @param hours      mandatory = yes. allowed values = {@code 0-23   * / , -}
     * @param dayOfMonth mandatory = yes. allowed values = {@code 1-31  * / , - ? L W}
     * @param month      mandatory = yes. allowed values = {@code 1-12 or JAN-DEC    * / , -}
     * @param dayOfWeek  mandatory = yes. allowed values = {@code 0-6 or SUN-SAT * / , - ? L #}
     * @param year       mandatory = no. allowed values = {@code 1970–2099    * / , -}
     * @return a CRON Formatted String.
     */
    public static String generateCronExpression(final String seconds, final String minutes, final String hours,
                                                final String dayOfMonth,
                                                final String month, final String dayOfWeek, final String year) {
        return String.format("%1$s %2$s %3$s %4$s %5$s %6$s %7$s", seconds, minutes, hours, dayOfMonth, month, dayOfWeek, year);
    }

    public String getDataPath(String directory) {
        String path = dataPath + "/" + directory;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        return path;
    }

}
