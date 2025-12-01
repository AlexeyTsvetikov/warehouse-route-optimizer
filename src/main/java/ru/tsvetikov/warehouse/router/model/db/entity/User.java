package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.tsvetikov.warehouse.router.model.enums.Role;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.PICKER;

    @Column(name = "last_known_x")
    private Double lastKnownX;

    @Column(name = "last_known_y")
    private Double lastKnownY;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();

}
