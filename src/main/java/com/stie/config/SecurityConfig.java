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

                // User Management (site admin)
                .antMatchers("/users/register", "/users/{id}/delete").hasAuthority("ROLE_ADMIN")

                // Tenant-scoped resources (departments, locations, categories, settings)
                .antMatchers("/departments/**", "/locations/**", "/api/departments/**", "/api/locations/**").hasAuthority("MANAGE_DEPARTMENTS")
                .antMatchers("/settings/**").hasAuthority("MANAGE_SETTINGS")

                // Jobs & Candidates
                .antMatchers("/jobs/new", "/jobs/create", "/jobs/edit/**", "/jobs/update").hasAuthority("CREATE_JOBS")
                .antMatchers("/jobs/*/approve", "/jobs/*/reject", "/jobs/approvals", "/jobs/pending").hasAuthority("APPROVE_JOBS")
                .antMatchers("/jobs/**").hasAnyAuthority("MANAGE_JOBS", "CREATE_JOBS", "APPROVE_JOBS", "MANAGE_APPLICANTS")
                .antMatchers("/candidates/**").hasAnyAuthority("MANAGE_APPLICANTS", "MANAGE_INTERVIEWS")

                // Interviews
                .antMatchers("/interviews/**").hasAuthority("MANAGE_INTERVIEWS")
                .antMatchers("/interviewer", "/interviewer/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "MANAGE_INTERVIEWS")

                // Reports
                .antMatchers("/reports/**").hasAuthority("VIEW_REPORTS")

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
