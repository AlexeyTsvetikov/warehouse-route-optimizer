package ru.tsvetikov.warehouse.router.model.db.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;

import java.util.Optional;


@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    boolean existsByCode(@NotBlank @Size(max = 100) String code);

    Page<Location> findAllByIsActiveTrue(Pageable pageRequest);

    Optional<Location> findByCode(String code);
}
