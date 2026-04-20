package ru.tsvetikov.warehouse.router.model.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Основной метод для операций с блокировкой
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "location"})
    @Query("SELECT s FROM Stock s WHERE s.product = :product AND s.location = :location")
    Optional<Stock> findByProductAndLocationWithLock(@Param("product") Product product,
                                                     @Param("location") Location location);

    // Метод для резервации (FIFO стратегия)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "location"})
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId " +
           "AND s.quantity > s.reservedQuantity " +
           "ORDER BY s.inboundDate ASC")
    List<Stock> findAvailableByProductIdFifoWithLock(@Param("productId") Long productId);

    // Опционально: read-only методы для отчетов (если нужны)
    @EntityGraph(attributePaths = {"product", "location"})
    @Query("SELECT s FROM Stock s WHERE s.quantity > 0")
    Page<Stock> findAllWithDetails(Pageable pageable);

    List<Stock> findByProductId(Long productId);

    @Query("SELECT s FROM Stock s WHERE (:locationCode = '' OR s.location.code = :locationCode) AND " +
           "(:search = '' OR LOWER(s.product.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Stock> findFiltered(@Param("locationCode") String locationCode, @Param("search") String search, Pageable pageable);
}
