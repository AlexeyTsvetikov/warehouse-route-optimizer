package ru.tsvetikov.warehouse.router.model.enums;

public enum LocationType {
    RECEIVING,
    BULK,  // Хранение паллет (Резерв)
    PICKING, // Штучный отбор (Активная зона)
    DISPATCH
}
