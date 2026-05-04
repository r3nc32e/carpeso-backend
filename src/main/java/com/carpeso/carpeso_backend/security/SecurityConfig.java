package com.carpeso.carpeso_backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of(
                            "http://localhost:5173",
                            "https://localhost:5173"
                    ));
                    config.setAllowedMethods(java.util.List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ Public auth endpoints
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/verify-registration").permitAll()
                        .requestMatchers("/api/auth/verify-otp").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/test").permitAll()
                        // ✅ Auth endpoints that require a logged-in user
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/profile").authenticated()
                        .requestMatchers("/api/auth/verify-password-change-otp").authenticated()
                        // ✅ Profile update allowed for all authenticated roles
                        .requestMatchers("/api/buyer/profile").hasAnyAuthority("BUYER", "ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/locations/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/superadmin/**").hasAuthority("SUPERADMIN")
                        .requestMatchers("/api/buyer/**").hasAuthority("BUYER")
                        .requestMatchers("/api/files/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}