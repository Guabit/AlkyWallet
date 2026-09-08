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

    /**
     * Define la cadena de filtros de seguridad HTTP y las reglas de autorizacion.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Manejo de sesion sin estado (Stateless con JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Endpoints publicos y protegidos
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos del frontend (HTML, CSS, JS, imágenes)
                        .requestMatchers("/",
                                "/index.html",
                                "/html/**",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/images/**"
                        ).permitAll()
                        // Endpoints públicos de autenticación y registro de usuarios
                        .requestMatchers("/api/auth/**", "/api/usuarios/registrar", "/error").permitAll()
                        // Webhook de Mercado Pago: lo llaman los servidores de Mercado Pago,
                        // no un usuario logueado, así que no puede requerir JWT.
                        .requestMatchers("/api/mercadopago/webhook").permitAll()
                        // Documentacion Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // El resto de la API requiere autenticación con JWT
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                // Registra el filtro JWT antes del filtro de autenticacion de Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
