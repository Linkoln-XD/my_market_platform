package com.market.order.web;

import com.market.order.cart.InsufficientStockException;
import com.market.order.order.EmptyCartException;
import com.market.order.catalog.CatalogConflictException;
import com.market.order.catalog.CatalogNotFoundException;
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
