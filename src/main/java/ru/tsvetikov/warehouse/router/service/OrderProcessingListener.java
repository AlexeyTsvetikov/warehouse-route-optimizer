package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.tsvetikov.warehouse.router.event.OrderProcessingStartedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingListener {

    private final OrderRepository orderRepository;
    private final WarehouseTaskManager warehouseTaskManager;

    @EventListener
    public void onOrderProcessingStarted(OrderProcessingStartedEvent event) {
        String orderNumber = event.orderNumber();
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Order with number '%s' not found", orderNumber), HttpStatus.NOT_FOUND));

        WarehouseTaskType taskType = order.getType() == OrderType.INBOUND
                ? WarehouseTaskType.RECEIVING
                : WarehouseTaskType.PICKING;

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            String sourceLocationCode = null;
            String targetLocationCode = null;

            if (taskType == WarehouseTaskType.PICKING) {
                sourceLocationCode = warehouseTaskManager.findLocationForProduct(product.getSku());
                if (sourceLocationCode == null) {
                    log.warn("No stock found for product {} when creating PICKING task", product.getSku());
                    continue;
                }
            } else {
                targetLocationCode = warehouseTaskManager.findDefaultReceivingLocation();
            }

            WarehouseTaskRequest request = WarehouseTaskRequest.builder()
                    .type(taskType)
                    .productSku(product.getSku())
                    .plannedQuantity(orderItem.getQuantity())
                    .sourceLocationCode(sourceLocationCode)
                    .targetLocationCode(targetLocationCode)
                    .orderNumber(orderNumber)
                    .build();

            warehouseTaskManager.createSingleTask(request);
        }
    }
}
