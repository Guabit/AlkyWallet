package com.alkywallet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion principal de seguridad para la API REST y autorizacion de rutas.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos del frontend (HTML, CSS, JS, imágenes) y ruta de error
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/html/**",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/images/**",
                                "/error",
                                "/error/**"
                        ).permitAll()
                        // Endpoints públicos de la API
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/usuarios/registrar",
                                "/api/mercadopago/webhook"
                        ).permitAll()
                        // Documentación Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Endpoints que requieren rol ADMIN (Prevenir IDOR)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/usuarios/{id}").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/usuarios/{id}").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/usuarios/{id}").hasRole("ADMIN")
                        // La API privada requiere autenticación JWT
                        .requestMatchers("/api/**").authenticated()
                        // Cualquier otra ruta (páginas inexistentes) se permite para que Spring despache el 404
                        .anyRequest().permitAll())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}