package com.salvatore.gymapp.security;

import com.salvatore.gymapp.service.GymSubscriptionService;
import com.salvatore.gymapp.service.SubscriptionService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SubscriptionService subscriptionService;
    private final GymSubscriptionService gymSubscriptionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

                if (!userDetails.isEnabled()) {
                    SecurityContextHolder.clearContext();
                    writeForbiddenResponse(response, "Account o palestra disattivata");
                    return;
                }

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    if (userDetails instanceof CustomUserPrincipal principal) {
                        String role = principal.getRole();

                        if ("USER".equals(role)) {
                            boolean validSubscription = subscriptionService.hasValidSubscription(principal.getId());

                            if (!validSubscription) {
                                SecurityContextHolder.clearContext();
                                writeForbiddenResponse(response, "Abbonamento scaduto o non presente");
                                return;
                            }
                        }
                        if ("MANAGER".equals(role)) {
                            boolean validGymSubscription = gymSubscriptionService
                                    .hasValidSubscription(principal.getGymId());

                            if (!validGymSubscription) {
                                SecurityContextHolder.clearContext();
                                writeForbiddenResponse(response, "Abbonamento palestra scaduto");
                                return;
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, "Token scaduto");
            return;
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, "Token non valido");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + escapeJson(message) + "\"}");
    }

    private void writeForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String value) {
        return value.replace("\"", "\\\"");
    }
}