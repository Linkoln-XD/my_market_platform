package com.market.order.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class OrderCreateRateLimitFilter extends OncePerRequestFilter {

    private final OrderCreateRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/v1/orders".equals(request.getRequestURI())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    rateLimitService.assertWithinLimit(authentication.getName());
                } catch (RateLimitExceededException ex) {
                    writeRateLimitResponse(response, ex);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response, RateLimitExceededException ex)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Rate limit exceeded");
        problem.setType(URI.create("https://market.local/problems/rate-limit"));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
