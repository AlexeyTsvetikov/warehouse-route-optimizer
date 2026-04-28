package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    Optional<Product> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND (LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchActive(@Param("query") String query, Pageable pageable);
}
