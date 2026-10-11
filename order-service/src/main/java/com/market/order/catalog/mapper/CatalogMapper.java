package com.market.order.catalog.mapper;

import com.market.order.catalog.domain.CategoryEntity;
import com.market.order.catalog.domain.ProductEntity;
import com.market.order.catalog.dto.CategoryResponse;
import com.market.order.catalog.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    CategoryResponse toCategoryResponse(CategoryEntity entity);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "stockQuantity", source = "stock.quantity")
    ProductResponse toProductResponse(ProductEntity entity);
}
