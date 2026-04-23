package ru.tsvetikov.warehouse.router.model.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.enums.Role;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    Page<User> findAllByIsActiveTrue(Pageable pageable);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchActive(String query, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.role = :role")
    Page<User> searchActiveByRole(@Param("role") Role role, Pageable pageable);
}
