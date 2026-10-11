package com.market.order.admin.web;

import com.market.order.order.OrderStatus;
import com.market.order.order.dto.OrderPageResponse;
import com.market.order.order.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin orders", description = "Read-only order audit (ADMIN role)")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;

    @GetMapping
    @Operation(summary = "List orders (newest first, optional status filter)")
    public OrderPageResponse listOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderQueryService.listAll(status, pageable);
    }
}
