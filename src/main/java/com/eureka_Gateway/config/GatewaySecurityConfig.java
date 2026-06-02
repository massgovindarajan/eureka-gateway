//package com.eureka_Gateway.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsConfigurationSource;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Slf4j
//@Configuration
//@EnableWebFluxSecurity
//public class GatewaySecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http
//            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//            .csrf(ServerHttpSecurity.CsrfSpec::disable)
//            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//            .authorizeExchange(exchange -> exchange
//                // ✅ Permit ALL — JWT validation is handled by JwtAuthFilter (GlobalFilter)
//                .anyExchange().permitAll()
//            );
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(List.of(
//            "http://localhost:4200",
//            "http://localhost:3000"
//        ));
//        config.setAllowedMethods(List.of(
//            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
//        ));
//        config.setAllowedHeaders(List.of("*"));
//        config.setExposedHeaders(List.of(
//            "Authorization", "X-Auth-Error", "X-Total-Count"
//        ));
//        config.setAllowCredentials(true);
//        config.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        log.info("CORS configured for origins: {}", config.getAllowedOrigins());
//        return source;
//    }
//}

//package com.eureka_Gateway.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//
//@Slf4j
//@Configuration
//@EnableWebFluxSecurity
//public class GatewaySecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//
//        http
//            .csrf(ServerHttpSecurity.CsrfSpec::disable)
//            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//            .authorizeExchange(exchange -> exchange
//                .anyExchange().permitAll()
//            );
//
//        return http.build();
//    }
//}

package com.eureka_Gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(Customizer.withDefaults())              // ← THIS was missing
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // CORS rules — Spring Security WebFlux reads this bean when
    // .cors(Customizer.withDefaults()) is called above.
    // Uses reactive UrlBasedCorsConfigurationSource (NOT the servlet one)
    // ─────────────────────────────────────────────────────────────────
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:4201",
                "https://medical-six-dun.vercel.app",
                "https://medical-65ek7uigk-massgovindarajans-projects.vercel.app"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setExposedHeaders(List.of(
                "Authorization", "Content-Type", "X-Total-Count"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}