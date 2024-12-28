package com.example.cashcard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/cashcards/**")
                        // enable RBAC replace .authenticated() with
                        .hasRole("CARD-OWNER"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        var userBuilder = User.builder();
        var sarah = userBuilder
                .username("sarah1")
                .password(passwordEncoder.encode("abc123"))
                .roles("CARD-OWNER")
                .build();

        var timOwnsNoCards = userBuilder
                .username("tim-owns-no-cards")
                .password(passwordEncoder.encode("def456"))
                .roles("NON-OWNER")
                .build();

        var kumar = userBuilder
                .username("kumar2")
                .password(passwordEncoder.encode("ghi789"))
                .roles("CARD-OWNER")
                .build();

        return new InMemoryUserDetailsManager(sarah, timOwnsNoCards, kumar);
    }
}
