package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseTaskRepository extends JpaRepository<WarehouseTask, Long> {
    boolean existsByTaskNumber(String taskNumber);

    @Query("SELECT MAX(wt.id) FROM WarehouseTask wt")
    Optional<Long> findMaxId();

    Page<WarehouseTask> findByAssignedUserIdAndStatusIn(Long assignedUserId, List<WarehouseTaskStatus> statuses,
                                                        Pageable pageable);

    Page<WarehouseTask> findByStatusIn(List<WarehouseTaskStatus> statuses, Pageable pageable);

}
