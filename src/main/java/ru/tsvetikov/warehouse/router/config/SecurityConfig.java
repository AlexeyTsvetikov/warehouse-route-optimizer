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
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/login", "/tsd/login").permitAll()
                        .requestMatchers("/redirect-after-login").authenticated()
                        .requestMatchers("/users/change-password/self").authenticated()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/tsd/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "PICKER", "DRIVER")
                        .requestMatchers("/categories/**", "/locations/**", "/products/**",
                                "/orders/**", "/stocks/**", "/tasks/**", "/dashboard")
                        .hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "PICKER", "DRIVER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            String tsd = request.getParameter("tsd");
                            if ("true".equals(tsd)) {
                                response.sendRedirect("/tsd/tasks");
                            } else {
                                response.sendRedirect("/dashboard");
                            }
                        })
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(86400) // 24 часа
                )
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String referer = request.getHeader("Referer");
                            if (referer != null && referer.contains("/tsd")) {
                                response.sendRedirect("/tsd/login");
                            } else {
                                response.sendRedirect("/login?logout");
                            }
                        })
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