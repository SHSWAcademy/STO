package server.main.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import server.main.global.security.JwtAccessDeniedHandler;
import server.main.global.security.JwtAuthenticationEntryPoint;
import server.main.global.security.JwtAuthenticationFilter;
import server.main.global.security.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CorsConfig corsConfig;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                /* 공개 url 규칙표 즉 비회원 */
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/ws/trading/**",
                                "/file/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/token",
                                "/api/token/*/chart",
                                "/api/token/*/info",
                                "/api/token/*/allocation",
                                "/api/token/*/disclosure",
                                "/api/token/*/tick-size",
                                "/api/token/*/candle",
                                "/api/token/*/trades",
                                "/api/disclosure",
                                "/api/notice",
                                "/api/notice/*"
                        ).permitAll()
                        .requestMatchers("/blockchain/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**", "/ws/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
                // 개발동안은 시큐리티 비활성화
//                http
//                .csrf(AbstractHttpConfigurer::disable)
//                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()
//                );
        return http.build();
    }
}
