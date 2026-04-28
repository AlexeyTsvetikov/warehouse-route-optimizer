package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;

import java.util.List;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Page<OrderItem> findByOrderId(Long orderId, Pageable pageable);

    boolean existsByOrderIdAndProductId(Long orderId, Long productId);

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}