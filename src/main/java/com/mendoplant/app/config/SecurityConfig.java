package com.mendoplant.app.config;

import com.mendoplant.app.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración central de Spring Security 6:
 *  - Form Login basado en sesión (HttpSession).
 *  - BCrypt para el hash de contraseñas.
 *  - Autorización por URL según el rol (ROLE_USER / ROLE_ADMIN).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos
                .requestMatchers("/", "/home", "/catalogo", "/terminos", "/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**", "/h2-console/**", "/.well-known/**").permitAll()
                // Blog y guías de cuidado
                .requestMatchers("/blog", "/blog/**").permitAll()
                // Webhook de Mercado Pago (público por diseño; se valida contra la API de MP)
                .requestMatchers("/api/pagos/**").permitAll()
                // Autenticación
                .requestMatchers("/auth/**", "/registro/**", "/login").permitAll()
                // Dashboard del vendedor: publicar, promociones y publicaciones propias.
                // "Envíos gratis" queda reservado al administrador.
                .requestMatchers("/dashboard/promociones/envios-gratis").hasRole("ADMIN")
                .requestMatchers("/dashboard/**").hasAnyRole("VENDEDOR", "ADMIN")
                // Acciones privadas sobre plantas: solo vendedores o admin
                .requestMatchers("/plantas/editar/**", "/plantas/eliminar/**",
                                 "/plantas/mis-publicaciones").hasAnyRole("VENDEDOR", "ADMIN")
                // Catálogo y detalle de plantas: público
                .requestMatchers("/plantas/**").permitAll()
                // Subrutas de tienda por categoría: público
                .requestMatchers("/tienda/**").permitAll()
                // Carrito: ver y modificar es anónimo; la compra final requiere login (lógica en CarritoController)
                .requestMatchers("/carrito", "/carrito/agregar", "/carrito/quitar/**", "/carrito/restar/**", "/carrito/vaciar", "/carrito/menu").permitAll()
                // Panel de administración
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Requieren estar autenticado
                .requestMatchers("/wishlist/**").authenticated()
                .requestMatchers("/ordenes/**").authenticated()
                .requestMatchers("/cuenta/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
            // Necesario para poder usar la consola H2 embebida en un iframe durante desarrollo
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/h2-console/**", "/api/pagos/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
