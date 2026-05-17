package com.yeswater.bids.export.infrastructure.security;

import com.yeswater.bids.export.infrastructure.web.InternalTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ExportSecurityConfig {

    @Bean
    public SecurityFilterChain exportSecurityFilterChain(HttpSecurity http, InternalTokenFilter internalTokenFilter)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
