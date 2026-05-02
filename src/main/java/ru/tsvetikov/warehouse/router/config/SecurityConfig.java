package ru.tsvetikov.warehouse.router.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Статика доступна всем
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // Страница логина
                        .requestMatchers("/login").permitAll()

                        .requestMatchers("/users/change-password/**").authenticated()

                        // Пользователи — только ADMIN
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // Справочники и заказы — просмотр для всех, управление для ADMIN/MANAGER
                        .requestMatchers("/categories/**", "/locations/**", "/products/**",
                                "/orders/**", "/stocks/**", "/tasks/**", "/dashboard")
                        .hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "PICKER", "DRIVER")

                        // API — все авторизованные
                        .requestMatchers("/api/**").authenticated()

                        // Защита от всего остального
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}