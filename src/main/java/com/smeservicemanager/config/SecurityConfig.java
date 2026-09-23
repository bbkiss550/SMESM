package com.smeservicemanager.config;

import com.smeservicemanager.security.DatabaseUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, DatabaseUserDetailsService userDetailsService) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/track/**", "/assets/**", "/error", "/actuator/health").permitAll()
                        .requestMatchers("/users/**", "/settings/**", "/reports/**").hasRole("ADMIN")
                        .requestMatchers("/my-jobs/**").hasRole("TECHNICIAN")
                        .requestMatchers("/dashboard", "/jobs/**", "/calendar/**", "/customers/**", "/services/**", "/products/**", "/stock/**", "/payments/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean technician = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));
                            response.sendRedirect(request.getContextPath() + (technician ? "/my-jobs" : "/dashboard"));
                        })
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .exceptionHandling(errors -> errors.accessDeniedPage("/error/403"))
                .sessionManagement(session -> session.sessionFixation().migrateSession());
        return http.build();
    }
}
