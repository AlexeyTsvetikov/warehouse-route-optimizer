package ru.tsvetikov.warehouse.router.model.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.entity.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // Метод с блокировкой для transfer
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "location"})
    @Query("SELECT s FROM Stock s WHERE s.product = :product AND s.location = :location")
    Optional<Stock> findByProductAndLocationWithLock(@Param("product") Product product,
                                                     @Param("location") Location location);

    // Для других методов сервиса (increase, decrease, reserve):
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "location"})
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId " +
            "AND s.quantity > s.reservedQuantity " +
            "ORDER BY s.inboundDate ASC")
    List<Stock> findAvailableByProductIdFifoWithLock(@Param("productId") Long productId);
}
