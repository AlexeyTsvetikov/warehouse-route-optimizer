package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderDetail;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.enums.OrderStatus;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
            "WHERE od.product = :product " +
            "AND od.order.status NOT IN :excludedStatuses")
    boolean existsByProductAndOrderStatusNotIn(@Param("product") Product product,
                                               @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    @Query("SELECT od FROM OrderDetail od WHERE od.order.id = :orderId")
    List<OrderDetail> findByOrderId(@Param("orderId") Long orderId);
}
