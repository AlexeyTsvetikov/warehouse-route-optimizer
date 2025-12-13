package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;


import java.util.List;
import java.util.Optional;


@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

}
