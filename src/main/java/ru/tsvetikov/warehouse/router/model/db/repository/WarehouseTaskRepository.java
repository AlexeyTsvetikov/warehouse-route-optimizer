package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.WarehouseTask;

import java.util.Optional;

@Repository
public interface WarehouseTaskRepository extends JpaRepository<WarehouseTask, Long> {
//
//    @Query("SELECT MAX(t.id) FROM WarehouseTask w")
//    Optional<Long> findMaxId();
//
//    boolean existsByTaskNumber(String newNumber);
}
