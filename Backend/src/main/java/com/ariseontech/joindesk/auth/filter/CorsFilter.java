package com.ariseontech.joindesk.auth.filter;

import com.ariseontech.joindesk.project.service.ConfigurationService;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

public class CorsFilter implements Filter {

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {
        ServletContext servletContext = req.getServletContext();
        WebApplicationContext webApplicationContext = WebApplicationContextUtils
                .getWebApplicationContext(servletContext);
        ConfigurationService configurationService = webApplicationContext.getBean(ConfigurationService.class);

        HttpServletRequest reqq = (HttpServletRequest) req;

        //reqq.getHeaderNames().asIterator().forEachRemaining(s -> System.out.println("Header: " + s + ", Val: " + reqq.getHeader(s)));

        String reqServer = reqq.getHeader("Origin");

        if (ObjectUtils.isEmpty(reqServer) && reqq.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(req, res);
            return;
        }

        String appDomain = configurationService.getApplicationDomain();

        if (reqServer == null || !reqServer.contains(appDomain))
            throw new AccessDeniedException("Error");

        HttpServletResponse response = (HttpServletResponse) res;
        System.out.println(reqServer);
        response.setHeader("Access-Control-Allow-Origin", reqServer);
        System.out.println("App domain: " + appDomain);
        if (appDomain != null &&
                !appDomain.contains("localhost"))
            response.setHeader("Access-Control-Allow-Origin", configurationService.getApplicationDomain());
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET,, PUT, DELETE, HEAD, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers",
                "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers,X-AUTH-TOKEN,X-JD-D,ACCPR, X-TZ");
        filterChain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig arg0) {

    }

}
