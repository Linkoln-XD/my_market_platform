package com.market.order.catalog.repository;

import com.market.order.catalog.domain.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySku(String sku);

    @EntityGraph(attributePaths = {"category", "stock"})
    Optional<ProductEntity> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"category", "stock"})
    @Query("""
            select p from ProductEntity p
            where (:categoryId is null or p.category.id = :categoryId)
              and (:activeOnly = false or p.active = true)
            """)
    Page<ProductEntity> findCatalogPage(
            @Param("categoryId") UUID categoryId,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable);
}
