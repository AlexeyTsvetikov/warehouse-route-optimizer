package ru.tsvetikov.warehouse.router.model.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.tsvetikov.warehouse.router.model.enums.CellType;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@Table(name = "rack_cells")
public class RackCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "storage_rack_id", nullable = false)
    private StorageRack storageRack;

    @Column(name = "cell_code", nullable = false, unique = true, length = 20)
    private String cellCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cell_type", nullable = false, length = 20)
    private CellType cellType;

    @Column(name = "coord_x", nullable = false)
    private Double coordX;

    @Column(name = "coord_y", nullable = false)
    private Double coordY;

    @Column(name = "max_volume", nullable = false)
    private Double maxVolume;

    @Column(name = "current_volume", nullable = false)
    private Double currentVolume = 0.0;

    @Column(name = "is_occupied", nullable = false)
    private boolean occupied = false;

    @OneToMany(mappedBy = "sourceCell", fetch = FetchType.LAZY)
    private Set<Task> sourceTasks = new HashSet<>();

    @OneToMany(mappedBy = "targetCell", fetch = FetchType.LAZY)
    private Set<Task> targetTasks = new HashSet<>();

}
