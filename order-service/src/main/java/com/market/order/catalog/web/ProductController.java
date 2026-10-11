package com.market.order.catalog.web;

import com.market.order.catalog.dto.CreateProductRequest;
import com.market.order.catalog.dto.ProductResponse;
import com.market.order.catalog.dto.UpdateProductRequest;
import com.market.order.catalog.dto.UpdateStockRequest;
import com.market.order.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.market.order.catalog.dto.ProductPageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catalog products and stock levels")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products (paged, optional category filter)")
    public ProductPageResponse listProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.listProducts(categoryId, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id")
    public ProductResponse getProduct(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product with initial stock")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product fields")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @PutMapping("/{id}/stock")
    @Operation(summary = "Set on-hand stock quantity")
    public ProductResponse updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockRequest request) {
        return productService.updateStock(id, request);
    }
}
