package com.grzechuhehe.SportsBettingManagerApp.config;

import com.grzechuhehe.SportsBettingManagerApp.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter{

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    
    private final JwtUtils jwtUtils;

    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.info("Processing request: {}, method: {}", path, request.getMethod());

        try {
            // Tylko próbujemy uwierzytelnić jeśli token jest obecny
            String jwt = parseJwt(request);
            
            if (jwt != null) {
                logger.info("JWT token found in request: {}", jwt);
                logger.info("Request URI: {}", request.getRequestURI());
                logger.info("Request method: {}", request.getMethod());
                
                // Dodajemy szczegółową diagnostykę walidacji tokena
                boolean isValid = jwtUtils.validateJwtToken(jwt);
                logger.info("JWT token is valid: {}", isValid);
                
                if (isValid) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.info("Username from token: {}", username);
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.info("User details loaded successfully: {}", userDetails.getUsername());
                    logger.info("User authorities: {}", userDetails.getAuthorities());
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("Authentication set in SecurityContext for user: {}", userDetails.getUsername());
                } else {
                    logger.warn("JWT token validation failed, removing any existing authentication");
                    SecurityContextHolder.clearContext();
                }
            } else {
                logger.info("No JWT token found in request for path: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage(), e);
            // Wypisz pełny stack trace
            e.printStackTrace();
        }
        
        // Wyświetl status autentykacji przed kontynuowaniem łańcucha
        logger.info("Authentication status before proceeding: {}", 
                SecurityContextHolder.getContext().getAuthentication() != null ? 
                "Authenticated as " + SecurityContextHolder.getContext().getAuthentication().getName() : 
                "Not authenticated");
        
        // Zawsze kontynuujemy łańcuch filtrów
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}