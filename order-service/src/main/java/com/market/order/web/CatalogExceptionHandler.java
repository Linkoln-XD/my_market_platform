package com.market.order.web;

import com.market.order.cart.InsufficientStockException;
import com.market.order.order.EmptyCartException;
import com.market.order.order.InvalidOrderStateException;
import com.market.order.order.OrderNotFoundException;
import com.market.order.catalog.CatalogConflictException;
import com.market.order.catalog.CatalogNotFoundException;
import com.market.order.order.OrderOwnerMismatchException;
import com.market.order.security.DemoTokenDisabledException;
import com.market.order.security.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    ProblemDetail handleNotFound(CatalogNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setType(URI.create("https://market.local/problems/not-found"));
        return problem;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order not found");
        problem.setType(URI.create("https://market.local/problems/order-not-found"));
        return problem;
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ProblemDetail handleInvalidOrderState(InvalidOrderStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Invalid order state");
        problem.setType(URI.create("https://market.local/problems/invalid-order-state"));
        return problem;
    }

    @ExceptionHandler(EmptyCartException.class)
    ProblemDetail handleEmptyCart(EmptyCartException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Cart is empty");
        problem.setType(URI.create("https://market.local/problems/empty-cart"));
        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Insufficient stock");
        problem.setType(URI.create("https://market.local/problems/insufficient-stock"));
        return problem;
    }

    @ExceptionHandler(OrderOwnerMismatchException.class)
    ProblemDetail handleOwnerMismatch(OrderOwnerMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://market.local/problems/forbidden"));
        return problem;
    }

    @ExceptionHandler(DemoTokenDisabledException.class)
    ProblemDetail handleDemoTokenDisabled(DemoTokenDisabledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Demo auth disabled");
        problem.setType(URI.create("https://market.local/problems/demo-auth-disabled"));
        return problem;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ProblemDetail handleRateLimit(RateLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Rate limit exceeded");
        problem.setType(URI.create("https://market.local/problems/rate-limit"));
        return problem;
    }

    @ExceptionHandler(CatalogConflictException.class)
    ProblemDetail handleConflict(CatalogConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://market.local/problems/conflict"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(CatalogExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setType(URI.create("https://market.local/problems/validation"));
        return problem;
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
