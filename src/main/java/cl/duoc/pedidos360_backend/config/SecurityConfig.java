package cl.duoc.pedidos360_backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // @Bean le indica a Spring que este metodo genera un componente (Bean) administrado
    // por el contenedor de IoC de Spring para configurar la cadena de filtros de seguridad.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Habilita el soporte de CORS en Spring Security para permitir peticiones desde Angular (localhost:4200)
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            // Permite desplegar la consola web de H2 (/h2-console) dentro de un iframe
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // Permite las solicitudes preflight (OPTIONS) que los navegadores envian antes de enviar el Bearer token
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Permite el acceso libre a la consola H2 en desarrollo
                .requestMatchers("/h2-console/**").permitAll()

                // =========================================================================
                // CONFIGURACION DE SEGURIDAD Y SCOPES
                // =========================================================================
                // OPCION 1 (Estándar con AWS API Gateway):
                // Exige que la peticion tenga un JWT valido emitido por AWS Cognito.
                // La validacion granular de Scopes (read, write, delete) se delega a API Gateway
                // en 'Method Request -> Authorization Scopes'.
                .requestMatchers("/api/pedidos", "/api/pedidos/**").authenticated()

                // OPCION 2 (Seguridad Zero-Trust: validacion explicita de Scopes en el Backend):
                // Si deseas validar los custom scopes de Cognito directamente en Spring Boot,
                // comenta la OPCION 1 y descomenta las siguientes reglas:
                // .requestMatchers(HttpMethod.GET, "/api/pedidos", "/api/pedidos/**")
                //     .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-read", "SCOPE_pedidos/read")
                // .requestMatchers(HttpMethod.POST, "/api/pedidos")
                //     .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-write", "SCOPE_rs-api-pedidos/pedidos-create", "SCOPE_pedidos/write")
                // .requestMatchers(HttpMethod.PUT, "/api/pedidos/**")
                //     .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-write", "SCOPE_rs-api-pedidos/pedidos-update", "SCOPE_pedidos/write")
                // .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**")
                //     .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-write", "SCOPE_rs-api-pedidos/pedidos-delete", "SCOPE_pedidos/delete")

                .anyRequest().permitAll()
            )
            // Configura la aplicacion como Resource Server OAuth 2.0.
            // Spring interceptara la cabecera 'Authorization: Bearer <JWT>' y decodificara el token.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Define la configuracion de origenes, metodos y cabeceras permitidas para evitar bloqueos por CORS en el navegador
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origen del frontend Angular de laboratorio
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
