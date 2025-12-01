package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.tsvetikov.warehouse.router.model.enums.RackType;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "storage_racks")
public class StorageRack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "zone", nullable = false, length = 20)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private RackType rackType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "storageRack", fetch = FetchType.LAZY)
    private Set<RackCell> rackCells = new LinkedHashSet<>();
}
