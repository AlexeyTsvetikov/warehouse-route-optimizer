package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsvetikov.warehouse.router.model.db.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
