package ru.tsvetikov.warehouse.router.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.tsvetikov.warehouse.router.event.OrderItemCompletedEvent;
import ru.tsvetikov.warehouse.router.event.OrderProcessingStartedEvent;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Order;
import ru.tsvetikov.warehouse.router.model.db.entity.OrderItem;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.entity.Stock;
import ru.tsvetikov.warehouse.router.model.db.repository.OrderRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.WarehouseTaskRequest;
import ru.tsvetikov.warehouse.router.model.enums.OrderType;
import ru.tsvetikov.warehouse.router.model.enums.WarehouseTaskType;
import ru.tsvetikov.warehouse.router.service.OrderService;
import ru.tsvetikov.warehouse.router.service.StockService;
import ru.tsvetikov.warehouse.router.service.WarehouseTaskManager;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingListener {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final WarehouseTaskManager warehouseTaskManager;
    private final StockService stockService;

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

            if (taskType == WarehouseTaskType.PICKING) {
                List<Stock> stocks = stockService.findAvailableStocksByProductSku(product.getSku());

                if (stocks.isEmpty()) {
                    log.warn("No stock found for product {}", product.getSku());
                    continue;
                }

                // Сортируем по Манхеттенскому расстоянию от (0,0)
                stocks.sort(Comparator.comparingDouble(s ->
                        Math.abs(s.getLocation().getCoordX()) +
                        Math.abs(s.getLocation().getCoordY())));

                int remaining = orderItem.getQuantity();
                for (Stock stock : stocks) {
                    if (remaining <= 0) break;

                    int available = stock.getQuantity() - stock.getReservedQuantity();
                    int toPick = Math.min(available, remaining);

                    if (toPick > 0) {
                        WarehouseTaskRequest request = WarehouseTaskRequest.builder()
                                .type(WarehouseTaskType.PICKING)
                                .productSku(product.getSku())
                                .plannedQuantity(toPick)
                                .sourceLocationCode(stock.getLocation().getCode())
                                .orderNumber(orderNumber)
                                .build();
                        warehouseTaskManager.createSingleTask(request);
                        remaining -= toPick;
                    }
                }

                if (remaining > 0) {
                    log.warn("Could not fulfill full quantity {} for product {}. Shortage: {}",
                            orderItem.getQuantity(), product.getSku(), remaining);
                }
            } else {
                // RECEIVING — как раньше
                String targetLocationCode = warehouseTaskManager.findNearestReceivingLocation(0.0, 0.0);
                WarehouseTaskRequest request = WarehouseTaskRequest.builder()
                        .type(taskType)
                        .productSku(product.getSku())
                        .plannedQuantity(orderItem.getQuantity())
                        .targetLocationCode(targetLocationCode)
                        .orderNumber(orderNumber)
                        .build();
                warehouseTaskManager.createSingleTask(request);
            }
        }
    }

    @EventListener
    public void onOrderItemCompleted(OrderItemCompletedEvent event) {
        OrderItem orderItem = event.orderItem();
        Order order = orderItem.getOrder();

        boolean allCompleted = order.getOrderItems().stream()
                .allMatch(OrderItem::isFullyCollected);

        if (allCompleted) {
            try {
                orderService.completeOrder(order.getOrderNumber());
                log.info("Order {} fully completed!", order.getOrderNumber());
            } catch (Exception e) {
                log.warn("Order {} not ready yet: {}", order.getOrderNumber(), e.getMessage());
            }
        }
    }
}
