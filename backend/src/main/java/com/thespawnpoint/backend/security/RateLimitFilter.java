package com.thespawnpoint.backend.security;

import com.thespawnpoint.backend.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIP(request);

        if (path.startsWith("/api/auth/login")) {
            applyRateLimit(request, response, filterChain, ip, "LOGIN");
        } else if (path.startsWith("/api/auth/register")) {
            applyRateLimit(request, response, filterChain, ip, "REGISTER");
        } else if (path.startsWith("/api/auth/forgot-password")) {
            applyRateLimit(request, response, filterChain, ip, "FORGOT_PASSWORD");
        } else if ((path.equals("/api/profile/avatar") || path.contains("/avatar")) && "POST".equalsIgnoreCase(request.getMethod())) {
            applyRateLimit(request, response, filterChain, ip, "UPLOAD");
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void applyRateLimit(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain,
                                String key,
                                String type) throws IOException, ServletException {

        Bucket bucket = rateLimitingService.resolveBucket(key, type);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            sendErrorResponse(response);
        }
    }

    private void sendErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Занадто багато запитів. Будь ласка, зачекайте.\",\"errorCode\":\"TOO_MANY_REQUESTS\"}");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
