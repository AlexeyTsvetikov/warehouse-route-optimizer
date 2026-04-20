package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findAllByIsActiveTrue(Pageable pageRequest);
    boolean existsByName(String name);
    Optional<Category> findByName(String name);

    @Query("SELECT c FROM Category c WHERE (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.description) " +
           "LIKE LOWER(CONCAT('%', :query, '%'))) AND c.isActive = true")
    Page<Category> searchActive(@Param("query") String query, Pageable pageable);
}
