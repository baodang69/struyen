package com.struyen.struyen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt bảo vệ CSRF (bắt buộc đối với các REST API độc lập dùng POST/PUT/DELETE)
                .csrf(AbstractHttpConfigurer::disable)
                // Cấu hình phân quyền HTTP
                .authorizeHttpRequests(auth -> auth
                        // Hiện tại hệ thống đang ở Phase 1/2 nên ta mở thả ga tất cả API để test
                        // Postman dễ dàng.
                        // Khi nào làm tới Phase Authentication (Admin Login) ta sẽ siết lại các API bắt
                        // đầu bằng /api/v1/admin/**
                        .anyRequest().permitAll());

        return http.build();
    }
}
