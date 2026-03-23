package com.thespawnpoint.backend.security;

import com.thespawnpoint.backend.entity.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BanAccessFilter extends OncePerRequestFilter {

    private static final String BANNED_ERROR_CODE = "BANNED";
    private static final String AUTH_PREFIX = "/api/auth";
    private static final String UNBAN_PREFIX = "/api/unban-requests";
    private static final String AVATAR_PREFIX = "/avatars/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user) || !user.isBanned()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedForBanned(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String banReason = user.getBanReason();
        if (banReason == null) {
            banReason = "";
        }
        banReason = banReason.replace("\\", "\\\\").replace("\"", "\\\"");

        response.getWriter().write("{\"message\":\"Користувач заблокований\",\"errorCode\":\""
                + BANNED_ERROR_CODE
                + "\",\"banReason\":\""
                + banReason
                + "\"}");
    }

    private boolean isAllowedForBanned(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }

        if (path.equals(AUTH_PREFIX + "/me") && HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }

        if (path.equals(AUTH_PREFIX + "/refresh") && HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        if (path.equals(AUTH_PREFIX + "/logout") && HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        if (path.startsWith(AVATAR_PREFIX) && HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }

        return path.startsWith(UNBAN_PREFIX)
                && (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.POST.matches(request.getMethod()));
    }
}



