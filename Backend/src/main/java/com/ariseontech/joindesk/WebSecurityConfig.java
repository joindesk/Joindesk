package com.ariseontech.joindesk;

import com.ariseontech.joindesk.auth.filter.TokenAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeRequests().antMatchers(HttpMethod.OPTIONS).permitAll();
        http.authorizeRequests().antMatchers(SystemInfo.apiPrefix + "/*").permitAll();
        http.authorizeRequests().antMatchers(SystemInfo.apiPrefix + "/auth/**").permitAll();
        http.authorizeRequests().antMatchers(SystemInfo.apiPrefix + "/media/**").permitAll();
        http.authorizeRequests().antMatchers(SystemInfo.apiPrefix + "/slack/**").permitAll();
        http.authorizeRequests().antMatchers(SystemInfo.apiPrefix + "/hook/**").permitAll();
        http.authorizeRequests().antMatchers(SystemInfo.apiPrefix + "/ws/**").permitAll();
        http.authorizeRequests().anyRequest().authenticated().and().logout().permitAll();
        http.logout().logoutRequestMatcher(new AntPathRequestMatcher("/api/logout")).invalidateHttpSession(true);
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.sessionManagement().sessionFixation().migrateSession();
        http.addFilterBefore(new TokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
