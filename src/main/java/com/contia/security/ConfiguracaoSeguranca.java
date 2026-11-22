package com.contia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracaoSeguranca {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAutenticacaoFilter tokenFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/teste").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/proprietarios", "/api/proprietarios/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/proprietarios/me").hasRole("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/proprietarios/senha").hasRole("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/funcionarios/login").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/vendas").hasAnyRole("PROPRIETARIO", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.GET, "/api/vendas/**").hasAnyRole("PROPRIETARIO", "FUNCIONARIO")
                        .requestMatchers("/api/vendas/**").hasRole("PROPRIETARIO")
                        .requestMatchers("/api/despesas/**", "/api/relatorios-financeiros/**",
                                "/api/produtos/**", "/api/estabelecimentos/**", "/api/funcionarios/**")
                                .hasRole("PROPRIETARIO")
                        .anyRequest().authenticated())
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
