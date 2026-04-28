package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;

import java.util.Optional;


@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Page<Location> findAllByIsActiveTrue(Pageable pageRequest);

    Optional<Location> findByCode(String code);

    @Query("SELECT l FROM Location l WHERE (LOWER(l.code) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.type) " +
           "LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Location> searchActive(@Param("query") String query, Pageable pageable);
}
