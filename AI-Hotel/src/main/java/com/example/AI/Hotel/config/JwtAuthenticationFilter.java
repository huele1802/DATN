package com.example.AI.Hotel.config;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final List<String> excludedPaths = Arrays.asList("/auth/**", "/login/oauth2/**", "/getAll/**");

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    //gọi các đường dẫn không cần xác thực
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String uri = request.getRequestURI();
//        // Bỏ qua các đường dẫn không yêu cầu xác thực
//        boolean isExcluded = uri.startsWith("/auth") || uri.startsWith("/login/oauth2") || uri.startsWith("/getAll");
//        // Không bỏ qua /hotels/search dù nó thuộc /hotels/**
//        boolean isHotelSearch = uri.equals("/hotels/search") || uri.startsWith("/hotels/search/");
//        return isExcluded && !isHotelSearch;
//    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Bỏ qua các đường dẫn không yêu cầu xác thực
        boolean isExcluded = uri.startsWith("/auth") || uri.startsWith("/login/oauth2") || uri.startsWith("/getAll") || uri.startsWith("/hotels") && !uri.equals("/hotels/search");
        return isExcluded;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (shouldNotFilter(request)) {
            chain.doFilter(request, response); // Bỏ qua filter cho các đường dẫn được liệt kê
            return;
        }
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing or invalid token format\", \"status\":401}");
            return;
        }
        String token = header.substring(7);

        try {
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authenticated user: " + email + ", authorities: " + userDetails.getAuthorities());
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Invalid token\", \"status\":401}");
                return;
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token has expired\", \"status\":401}");
            return;
        } catch (MalformedJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Malformed token\", \"status\":401}");
            return;
        } catch (UnsupportedJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unsupported token\", \"status\":401}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Authentication failed: " + e.getMessage() + "\", \"status\":401}");
            return;
        }

        chain.doFilter(request, response);
    }
}