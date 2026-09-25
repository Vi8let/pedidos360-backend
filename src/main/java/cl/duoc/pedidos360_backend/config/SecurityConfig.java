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
            .authorizeHttpRequests(auth -> auth
                // Permite las solicitudes preflight (OPTIONS) que los navegadores envian antes de enviar el Bearer token
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Seccion 20.3: Exige que cualquier peticion a /api/pedidos este autenticada con un JWT valido.
                // NOTA (Seccion 21): Si se requiere validar scopes especificos de Cognito, se puede cambiar a:
                // .requestMatchers("/api/pedidos").hasAuthority("SCOPE_<CUSTOM_SCOPE>")
                .requestMatchers("/api/pedidos").authenticated()
                .anyRequest().permitAll()
            )
            // Configura la aplicacion como Resource Server OAuth 2.0.
            // Spring interceptara la cabecera 'Authorization: Bearer <JWT>' y decodificara el token.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

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
