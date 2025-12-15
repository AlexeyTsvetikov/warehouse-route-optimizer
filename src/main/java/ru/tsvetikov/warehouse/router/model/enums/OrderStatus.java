package ru.tsvetikov.warehouse.router.model.enums;

public enum OrderStatus {
    NEW,            // Заказ создан, но задачи еще не сформированы
    PROCESSING,     // Задачи созданы, сборщики работают
    COMPLETED,      // Все собрано
    CANCELLED
}
