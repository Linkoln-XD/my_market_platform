package com.market.order.order.web;

import com.market.order.order.dto.CancelOrderRequest;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.order.dto.OrderResponse;
import com.market.order.order.service.OrderCancelService;
import com.market.order.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.market.order.order.OrderOwnerMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Orders", description = "Order checkout (payment follows Kafka on step 4+)")
public class OrderController {

    private final OrderService orderService;
    private final OrderCancelService orderCancelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create order from Redis cart", description = """
            Requires header Idempotency-Key. Order status is PENDING_PAYMENT.
            Stock is validated but reserved asynchronously via catalog.events later.
            """)
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @Parameter(in = ParameterIn.HEADER, required = true)
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Size(min = 8, max = 128)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
            String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        assertOwnerMatchesJwt(request.ownerId(), jwt);
        return orderService.createOrder(request, idempotencyKey);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order and publish OrderCancelled for compensations")
    public OrderResponse cancelOrder(
            @PathVariable UUID orderId,
            @RequestBody(required = false) CancelOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CancelOrderRequest body = request == null ? new CancelOrderRequest(null) : request;
        return orderCancelService.cancelOrder(orderId, body, jwt.getSubject());
    }

    private static void assertOwnerMatchesJwt(String ownerId, Jwt jwt) {
        if (jwt == null || !ownerId.equals(jwt.getSubject())) {
            throw new OrderOwnerMismatchException("ownerId must match the authenticated JWT subject");
        }
    }
}
