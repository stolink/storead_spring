package com.stolink.backend.global.auth.filter;

import com.stolink.backend.global.auth.oauth2.CustomOAuth2User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HeaderInjectionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // Skip logging for health checks or static resources to reduce noise
        if (!path.startsWith("/actuator") && !path.startsWith("/favicon.ico")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("Processing request: {} {}", request.getMethod(), path);
            log.info("Cookies: {}", request.getCookies() != null ? Arrays.toString(request.getCookies()) : "null");

            if (authentication != null) {
                log.info("Authenticated user: {}, Principal type: {}", authentication.getName(),
                        authentication.getPrincipal().getClass().getName());
                UUID userId = null;
                Object principal = authentication.getPrincipal();

                if (principal instanceof CustomOAuth2User oAuth2User) {
                    userId = oAuth2User.getUserId();
                } else if (principal instanceof com.stolink.backend.domain.user.entity.User user) {
                    userId = user.getId();
                }

                if (userId != null) {
                    log.info("Injecting X-User-Id: {}", userId);
                    HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(request);
                    requestWrapper.addHeader("X-User-Id", userId.toString());
                    filterChain.doFilter(requestWrapper, response);
                    return;
                } else {
                    log.warn("Could not extract userId from principal");
                }
            } else {
                log.info("SecurityContext Authentication is null");
            }
        }
        filterChain.doFilter(request, response);
    }

    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> headerMap = new HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = headerMap.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(headerMap.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String headerValue = headerMap.get(name);
            if (headerValue != null) {
                return Collections.enumeration(Collections.singletonList(headerValue));
            }
            return super.getHeaders(name);
        }
    }
}
