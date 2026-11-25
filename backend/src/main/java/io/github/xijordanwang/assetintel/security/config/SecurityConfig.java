package io.github.xijordanwang.assetintel.security.config;

import io.github.xijordanwang.assetintel.security.service.Actor;
import io.github.xijordanwang.assetintel.security.service.AppUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static io.github.xijordanwang.assetintel.security.entity.UserRole.*;

/**
 * Visitors sign in first (by picking a demo person); only the sign-in API, health check and the static
 * React files are public. Every API call needs a signed-in user.
 * Broad role checks live here; step-level rules (which plant manager, which coordinator) live in {@link Actor}.
 * Sessions use a cookie, and the React app sends the CSRF token from the XSRF-TOKEN cookie as a header.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null); // load the token on every request so the cookie is always set

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(c -> c.securityContextRepository(securityContextRepository()))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/users/**").hasRole(ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/requests", "/api/requests/*/submit")
                            .hasAnyRole(REQUESTER.name(), PLANT_MANAGER.name(), ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/requests/*/capitalise")
                            .hasAnyRole(FINANCE_CONTROLLER.name(), ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/changes", "/api/changes/*/submit")
                            .hasAnyRole(REQUESTER.name(), PLANT_MANAGER.name(), ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/stocktakes/*/sites/**")
                            .hasAnyRole(SITE_COORDINATOR.name(), ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/stocktakes", "/api/stocktakes/*/*")
                            .hasAnyRole(FINANCE_CONTROLLER.name(), ADMIN.name())
                        // what each role can see: reports for managers and finance, workflows for the people in them
                        .requestMatchers("/api/dashboard/**")
                            .hasAnyRole(PLANT_MANAGER.name(), FINANCE_CONTROLLER.name(), CFO.name(), ADMIN.name())
                        .requestMatchers("/api/requests/**", "/api/changes/**")
                            .hasAnyRole(REQUESTER.name(), PLANT_MANAGER.name(), FINANCE_CONTROLLER.name(), CFO.name(), ADMIN.name())
                        .requestMatchers("/api/stocktakes/**")
                            .hasAnyRole(SITE_COORDINATOR.name(), PLANT_MANAGER.name(), FINANCE_CONTROLLER.name(), ADMIN.name())
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/**").permitAll() // the React app and its assets
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AppUserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** Touches the CSRF token so its cookie is written on every response, including the first page load. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
