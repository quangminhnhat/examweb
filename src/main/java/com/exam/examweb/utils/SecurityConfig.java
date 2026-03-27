package com.exam.examweb.utils;

import com.exam.examweb.entities.User;
import com.exam.examweb.services.OAuthService;
import com.exam.examweb.services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
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
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/css/**", "/js/**", "/images/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(@NotNull HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/",
                                "/oauth/**", "/register", "/error", "/role-selection",
                                "/forgot-password", "/reset-password")
                        .permitAll()
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers("/api/management/**").hasAuthority("admin")
                        .requestMatchers("/admin/**").hasAuthority("admin")
                        .requestMatchers("/teacher/**").hasAnyAuthority("teacher", "admin")
                        .requestMatchers("/student/**").hasAnyAuthority("student", "admin")
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
                                            
                                            // Check if user has roles
                                            Optional<User> userOptional = userService.findByUsername(oidcUser.getEmail());
                                            if (userOptional.isPresent() && userOptional.get().getRoles().isEmpty()) {
                                                // No roles, redirect to selection
                                                response.sendRedirect("/role-selection");
                                            } else {
                                                // Has roles, proceed normally
                                                // Use EMAIL as the username for the token, as that is what is stored in the DB
                                                String token = tokenProvider.generateToken(oidcUser.getEmail());
                                                Cookie cookie = new Cookie("JWT_TOKEN", token);
                                                cookie.setHttpOnly(true);
                                                cookie.setPath("/");
                                                cookie.setMaxAge(7 * 24 * 60 * 60);
                                                response.addCookie(cookie);
                                                response.sendRedirect("/");
                                            }
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
