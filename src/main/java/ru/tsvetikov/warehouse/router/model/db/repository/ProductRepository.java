package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

}
