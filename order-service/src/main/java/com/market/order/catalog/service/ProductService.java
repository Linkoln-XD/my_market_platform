package com.market.order.catalog.service;

import com.market.order.catalog.CatalogConflictException;
import com.market.order.catalog.CatalogNotFoundException;
import com.market.order.catalog.cache.CatalogCacheNames;
import com.market.order.catalog.domain.CategoryEntity;
import com.market.order.catalog.domain.ProductEntity;
import com.market.order.catalog.domain.StockEntity;
import com.market.order.catalog.dto.CreateProductRequest;
import com.market.order.catalog.dto.ProductPageResponse;
import com.market.order.catalog.dto.ProductResponse;
import com.market.order.catalog.dto.UpdateProductRequest;
import com.market.order.catalog.dto.UpdateStockRequest;
import com.market.order.catalog.mapper.CatalogMapper;
import com.market.order.catalog.repository.CategoryRepository;
import com.market.order.catalog.repository.ProductRepository;
import com.market.order.catalog.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockRepository stockRepository;
    private final CatalogMapper catalogMapper;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CatalogCacheNames.PRODUCT_BY_ID,
            key = "#id",
            unless = "#result == null")
    public ProductResponse getProduct(UUID id) {
        return productRepository.findWithDetailsById(id)
                .map(catalogMapper::toProductResponse)
                .orElseThrow(() -> new CatalogNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CatalogCacheNames.PRODUCTS,
            key = "T(java.util.Objects).hash(#categoryId, #activeOnly, #pageable.pageNumber, #pageable.pageSize, #pageable.sort)")
    public ProductPageResponse listProducts(UUID categoryId, boolean activeOnly, Pageable pageable) {
        var page = productRepository.findCatalogPage(categoryId, activeOnly, pageable)
                .map(catalogMapper::toProductResponse);
        return new ProductPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Transactional
    @CacheEvict(cacheNames = CatalogCacheNames.PRODUCTS, allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new CatalogConflictException("Product SKU already exists: " + request.sku());
        }
        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CatalogNotFoundException("Category not found: " + request.categoryId()));

        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPriceCents(request.priceCents());
        product.setCurrency(request.currency().toUpperCase());
        product.setActive(true);
        productRepository.save(product);

        StockEntity stock = new StockEntity();
        stock.setProduct(product);
        stock.setQuantity(request.initialStock());
        stockRepository.save(stock);

        return productRepository.findWithDetailsById(product.getId())
                .map(catalogMapper::toProductResponse)
                .orElseThrow();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CatalogCacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CatalogCacheNames.PRODUCT_BY_ID, key = "#id")
    })
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        ProductEntity product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Product not found: " + id));
        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CatalogNotFoundException("Category not found: " + request.categoryId()));

        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPriceCents(request.priceCents());
        product.setCurrency(request.currency().toUpperCase());
        product.setActive(request.active());

        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CatalogCacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CatalogCacheNames.PRODUCT_BY_ID, key = "#id")
    })
    public ProductResponse updateStock(UUID id, UpdateStockRequest request) {
        ProductEntity product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Product not found: " + id));
        StockEntity stock = product.getStock();
        if (stock == null) {
            throw new CatalogNotFoundException("Stock not found for product: " + id);
        }
        stock.setQuantity(request.quantity());
        stockRepository.save(stock);
        return catalogMapper.toProductResponse(product);
    }
}
