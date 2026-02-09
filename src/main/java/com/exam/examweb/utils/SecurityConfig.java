package com.exam.examweb.utils;

import com.exam.examweb.services.OAuthService;
import com.exam.examweb.services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuthService oAuthService;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService());
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(@NotNull HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/",
                                "/oauth/**", "/register", "/error")
                        .permitAll()
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers("/api/management/**").hasAuthority("admin")
                        .requestMatchers("/admin/**")
                        .hasAnyAuthority("admin")
                        .requestMatchers("/teacher/**")
                        .hasAnyAuthority("teacher")
                        .requestMatchers("/student/**")
                        .hasAnyAuthority("student")
                        .anyRequest().authenticated()
                ).logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .deleteCookies("JSESSIONID", "JWT_TOKEN")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                ).formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            String token = tokenProvider.generateToken(authentication.getName());
                            Cookie cookie = new Cookie("JWT_TOKEN", token);
                            cookie.setHttpOnly(true);
                            cookie.setPath("/");
                            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
                            response.addCookie(cookie);
                            response.sendRedirect("/");
                        })
                        .failureUrl("/login?error")
                        .permitAll()
                ).oauth2Login(
                        oauth2Login -> oauth2Login
                                .loginPage("/login")
                                .failureUrl("/login?error")
                                .userInfoEndpoint(userInfoEndpoint ->
                                        userInfoEndpoint
                                                .oidcUserService(oAuthService)
                                )
                                .successHandler(
                                        (request, response,
                                         authentication) -> {
                                            var oidcUser =
                                                    (DefaultOidcUser) authentication.getPrincipal();

                                            userService.saveOauthUser(oidcUser.getEmail(), oidcUser.getName(), oidcUser.getSubject(), oidcUser.getFullName(), oidcUser.getPicture());

                                            // Generate JWT for OAuth user too
                                            String token = tokenProvider.generateToken(oidcUser.getName());
                                            Cookie cookie = new Cookie("JWT_TOKEN", token);
                                            cookie.setHttpOnly(true);
                                            cookie.setPath("/");
                                            cookie.setMaxAge(7 * 24 * 60 * 60);
                                            response.addCookie(cookie);

                                            response.sendRedirect("/");
                                        }
                                )
                                .permitAll()
                ).rememberMe(rememberMe -> rememberMe
                        .key("hutech")
                        .rememberMeCookieName("hutech")
                        .tokenValiditySeconds(24 * 60 * 60)
                        .userDetailsService(userDetailsService())
                ).exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedPage("/403")
                ).sessionManagement(sessionManagement ->
                        sessionManagement
                                .maximumSessions(1)
                                .expiredUrl("/login")
                ).httpBasic(httpBasic -> httpBasic
                        .realmName("hutech")
                ).build();
    }
}
