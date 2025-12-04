package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.StorageRack;

import java.util.List;

@Repository
public interface StorageRackRepository extends JpaRepository<StorageRack, Long> {

    List<StorageRack> findByZone(String zone);
}
