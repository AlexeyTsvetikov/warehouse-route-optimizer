package ru.tsvetikov.warehouse.router.model.db.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
