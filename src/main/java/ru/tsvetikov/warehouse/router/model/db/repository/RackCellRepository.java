package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.RackCell;


import java.util.List;
import java.util.Optional;


@Repository
public interface RackCellRepository extends JpaRepository<RackCell, Long> {

    boolean existsByCellCode(String s);

    List<RackCell> findByStorageRackId(Long rackId);

    List<RackCell> findByOccupiedFalse();

    Optional<RackCell> findByCellCode(String s);
}
