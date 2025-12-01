package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
