package com.market.order.catalog.service;

import com.market.order.catalog.CatalogConflictException;
import com.market.order.catalog.CatalogNotFoundException;
import com.market.order.catalog.domain.CategoryEntity;
import com.market.order.catalog.dto.CreateProductRequest;
import com.market.order.catalog.mapper.CatalogMapper;
import com.market.order.catalog.repository.CategoryRepository;
import com.market.order.catalog.repository.ProductRepository;
import com.market.order.catalog.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final UUID CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private CatalogMapper catalogMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("createProduct rejects duplicate SKU")
    void createProductRejectsDuplicateSku() {
        when(productRepository.existsBySku("BRG-CLASSIC")).thenReturn(true);

        CreateProductRequest request = new CreateProductRequest(
                CATEGORY_ID, "BRG-CLASSIC", "Duplicate", null, 500, "USD", 10);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("BRG-CLASSIC");
    }

    @Test
    @DisplayName("createProduct requires existing category")
    void createProductRequiresCategory() {
        when(productRepository.existsBySku("NEW-SKU")).thenReturn(false);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        CreateProductRequest request = new CreateProductRequest(
                CATEGORY_ID, "NEW-SKU", "New product", null, 500, "USD", 10);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessageContaining(CATEGORY_ID.toString());
    }
}
