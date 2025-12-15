package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findAllByIsActiveTrue(Pageable pageRequest);
    boolean existsByName(String name);
    Optional<Category> findByName(String name);
}
