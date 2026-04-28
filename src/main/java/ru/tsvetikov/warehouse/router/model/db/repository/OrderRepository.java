package ru.tsvetikov.warehouse.router.model.db.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.destinationRegion) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Order> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT MAX(o.id) FROM Order o")
    Optional<Long> findMaxId();
}
