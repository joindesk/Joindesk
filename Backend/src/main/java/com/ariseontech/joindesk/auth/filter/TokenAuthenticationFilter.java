package com.ariseontech.joindesk.auth.filter;


import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.domain.Token;
import com.ariseontech.joindesk.auth.service.AuthService;
import com.ariseontech.joindesk.auth.service.IPWhiteBlackListingService;
import com.ariseontech.joindesk.auth.util.CurrentLogin;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Transactional
public class TokenAuthenticationFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String authToken = Optional.ofNullable(httpRequest.getHeader("X-AUTH-TOKEN"))
                .orElse(Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                        .filter(c -> c.getName().equals("jdt")).map(Cookie::getValue).findAny().orElse(null));
        String apiToken = Optional.ofNullable(httpRequest.getHeader("X-API-TOKEN"))
                .orElse(Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                        .filter(c -> c.getName().equals("jda")).map(Cookie::getValue).findAny().orElse(null));
        String deviceInfo = Optional.ofNullable(httpRequest.getHeader("X-JD-D"))
                .orElse(Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                        .filter(c -> c.getName().equals("jdd")).map(Cookie::getValue).findAny().orElse(null));

        String requestUri = httpRequest.getRequestURI();
        logger.debug("Hitting " + requestUri);
        ServletContext servletContext = request.getServletContext();
        WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getWebApplicationContext(servletContext);
        AuthService authService = webApplicationContext.getBean(AuthService.class);
        IPWhiteBlackListingService ipWhiteBlackListingService = webApplicationContext.getBean(IPWhiteBlackListingService.class);
        CurrentLogin currentLogin = webApplicationContext.getBean(CurrentLogin.class);
        boolean skip = requestUri.startsWith("/auth/login") || requestUri.startsWith("/auth/register");
        if (requestUri.startsWith("/ws") ||
                requestUri.equalsIgnoreCase("/error") || requestUri.equalsIgnoreCase("/api/error")) {
            skip = true;
        }
        if (!skip) {
            if (apiToken == null && httpRequest.getCookies() != null) {
                List<Cookie> cookies = Arrays.asList(httpRequest.getCookies());
                Optional<Cookie> jdt = cookies.stream().filter(c -> c.getName().equalsIgnoreCase("jdt")).findAny();
                Optional<Cookie> jdx = cookies.stream().filter(c -> c.getName().equalsIgnoreCase("jdx")).findAny();
                Optional<Cookie> jdd = cookies.stream().filter(c -> c.getName().equalsIgnoreCase("jdd")).findAny();
                if (requestUri.contains("attachment") && requestUri.contains("/preview") && jdt.isPresent() && jdx.isPresent() && jdd.isPresent()) {
                    authToken = jdt.get().getValue() + jdx.get().getValue();
                    deviceInfo = URLDecoder.decode(jdd.get().getValue());
                }
            }
            logger.debug("Token Verification");
            if (apiToken != null && !apiToken.isEmpty()) {
                String currentIP = HelperUtil.getClientIp(httpRequest);
                if (!ipWhiteBlackListingService.isAllowedApi(currentIP)) {
                    logger.info(" Access denied from current location");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                    return;
                }
                Login login = authService.getMemberByToken(apiToken);
                if (null == login) {
                    logger.info(" Access denied - token not found");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                    return;
                }
                if (login.isPendingActivation()) {
                    logger.info(" Account not activated, Please contact Administrator");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                    return;
                }
                if (!login.isActive()) {
                    logger.info(" Account inactive, Please contact Administrator");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                    return;
                }
                if (login.isLocked()) {
                    logger.info(" Account locked");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                    return;
                }
                currentLogin.setUser(login);
                currentLogin.setAuthorities(authService.getAuthorities(login));
                currentLogin.setToken(apiToken);
                currentLogin.setAuthoritiesGlobal(authService.getGlobalAuthorities(login));
                User u = new User(login.getUserName(), "", new HashSet<GrantedAuthority>());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(u,
                        u.getPassword(), u.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("API Access Granted");
            } else if (authToken != null && !authToken.isEmpty()) {
                Token t;
                try {
                    t = authService.getToken(authToken);
                } catch (IllegalArgumentException e) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Session");
                    return;
                } catch (ExpiredJwtException e) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session Expired");
                    return;
                }
                if (null != t && !ObjectUtils.isEmpty(deviceInfo)) {
                    if (!deviceInfo.equalsIgnoreCase(t.getFp())) {
                        logger.info(" Access denied due to mismatching device info");
                        logger.info("Org: " + t.getFp() + " Sent: " + deviceInfo);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                        return;
                    }
                    Login l = authService.getMemberByUserNameIgnoreCase(t.getUser().getUserName());
                    currentLogin.setUser(l);
                    currentLogin.setAuthorities(authService.getAuthorities(l));
                    currentLogin.setToken(t.getToken());
                    currentLogin.setAuthoritiesGlobal(authService.getGlobalAuthorities(l));
                    User u = new User(l.getUserName(), "", new HashSet<>());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(u,
                            u.getPassword(), u.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Access Granted");
                } else {
                    logger.info(" Access denied - token mismatch");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
                    return;
                }
            } else {
                logger.debug("No Token");
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
