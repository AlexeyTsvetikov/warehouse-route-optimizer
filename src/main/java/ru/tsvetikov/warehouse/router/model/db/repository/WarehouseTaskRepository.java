package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseTaskRepository extends JpaRepository<WarehouseTask, Long> {
    boolean existsByTaskNumber(String taskNumber);

    @Query("SELECT MAX(wt.id) FROM WarehouseTask wt")
    Optional<Long> findMaxId();

    Page<WarehouseTask> findByAssignedUserIdAndStatusIn(Long assignedUserId, List<WarehouseTaskStatus> statuses,
                                                        Pageable pageable);

    boolean existsByOrderOrderNumberAndProductSku(String orderNumber, String productSku);

    @Query("SELECT t FROM WarehouseTask t " +
           "LEFT JOIN t.product p " +
           "LEFT JOIN t.order o " +
           "WHERE LOWER(t.taskNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.type) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<WarehouseTask> search(@Param("query") String query, Pageable pageable);

    Page<WarehouseTask> findByStatus(WarehouseTaskStatus status, Pageable pageable);

    @Query("SELECT t FROM WarehouseTask t WHERE t.order.orderNumber = :orderNumber AND t.type = :type")
    List<WarehouseTask> findByOrderNumberAndType(@Param("orderNumber") String orderNumber, @Param("type") WarehouseTaskType type);
}
