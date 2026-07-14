package com.stie.config;

import com.stie.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers().frameOptions().sameOrigin()
            .and()
            .csrf().disable()
            .authorizeRequests()
                // Public access
                .antMatchers("/", "/landing", "/*/landing", "/walkin", "/walkin/**", "/*/walkin", "/*/walkin/**",
                             "/apply/**", "/*/apply/**",
                             "/api/webhooks/**", "/css/**", "/js/**", "/debug/**").permitAll()

                // SuperAdmin only
                .antMatchers("/super-admin", "/super-admin/**", "/api/tenants/**").hasAuthority("ROLE_SUPER_ADMIN")

                // User Management - admin only
                .antMatchers("/users/register", "/users/{id}/delete").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                // Roles management - always accessible to ROLE_ADMIN so they can manage permissions
                .antMatchers("/settings/roles", "/settings/roles/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "System Settings")

                // Tenant-scoped resources (departments, locations, categories, settings)
                .antMatchers("/departments/**", "/locations/**", "/api/departments/**", "/api/locations/**")
                    .hasAnyAuthority("System Settings", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                .antMatchers("/settings/**")
                    .hasAnyAuthority("System Settings", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                // Jobs & Candidates
                .antMatchers("/jobs/new", "/jobs/create", "/jobs/edit/**", "/jobs/update")
                    .hasAnyAuthority("CREATE_JOBS", "ROLE_SUPER_ADMIN")
                .antMatchers("/jobs/approvals", "/jobs/pending", "/jobs/*/approve", "/jobs/*/reject")
                    .hasAnyAuthority("APPROVE_JOBS", "ROLE_SUPER_ADMIN", "ROLE_HR", "ROLE_ADMIN")
                .antMatchers("/jobs/**")
                    .hasAnyAuthority("Jobs", "MANAGE_JOBS", "CREATE_JOBS", "APPROVE_JOBS", "ROLE_SUPER_ADMIN")
                .antMatchers("/candidates/**")
                    .hasAnyAuthority("Job Applications", "Pipeline Kanban", "Candidate Database", "Interviews", "ROLE_SUPER_ADMIN")

                // Interviews
                .antMatchers("/interviews/**")
                    .hasAnyAuthority("Interviews", "ROLE_SUPER_ADMIN")
                .antMatchers("/interviewer", "/interviewer/**")
                    .hasAnyAuthority("Interviews", "ROLE_SUPER_ADMIN")

                // Reports
                .antMatchers("/reports/**")
                    .hasAnyAuthority("Reports", "ROLE_SUPER_ADMIN")

                // Hired Records
                .antMatchers("/hired-records/**")
                    .hasAnyAuthority("Hired Records", "ROLE_SUPER_ADMIN")

                // Everything else requires login
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            .and()
            .logout()
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            .and()
            .exceptionHandling()
                .accessDeniedPage("/access-denied");

        return http.build();
    }
}
