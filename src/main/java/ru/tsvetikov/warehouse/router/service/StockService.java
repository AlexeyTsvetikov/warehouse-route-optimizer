package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.entity.Stock;
import ru.tsvetikov.warehouse.router.model.db.repository.StockRepository;
import ru.tsvetikov.warehouse.router.model.dto.response.StockResponse;
import ru.tsvetikov.warehouse.router.model.mapper.StockMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final ProductService productService;
    private final LocationService locationService;
    private final StockMapper stockMapper;

    @Transactional(readOnly = true)
    public List<StockResponse> getByProduct(Long productId) {
        List<Stock> stocks = stockRepository.findByProductId(productId);
        return stocks.stream()
                .map(stockMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StockResponse> getFiltered(String locationCode, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        String loc = (locationCode != null) ? locationCode : "";
        String query = (search != null) ? search : "";

        Page<Stock> stocks = stockRepository.findFiltered(loc, query, pageable);
        return stocks.map(stockMapper::toResponse);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = DataIntegrityViolationException.class)
    public void increaseStock(String productSku, String locationCode, Integer quantity) {
        validateQuantity(quantity);

        log.debug("Increasing stock: {} @ {} +{}", productSku, locationCode, quantity);

        Product product = productService.getBySku(productSku);
        Location location = locationService.getByCode(locationCode);

        Stock stock = stockRepository.findByProductAndLocationWithLock(product, location)
                .orElseGet(() -> createStockWithRetry(product, location));

        stock.setQuantity(stock.getQuantity() + quantity);

        log.info("Increased stock: {} @ {} +{}. New total: {}",
                productSku, locationCode, quantity, stock.getQuantity());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void decreaseStock(String productSku, String locationCode, Integer quantity) {
        validateQuantity(quantity);

        Product product = productService.getBySku(productSku);
        Location location = locationService.getByCode(locationCode);

        Stock stock = stockRepository.findByProductAndLocationWithLock(product, location)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Stock not found for product %s at location %s", productSku, locationCode),
                        HttpStatus.NOT_FOUND));

        // Сначала снимаем с резерва, потом с количества
        int fromReserve = Math.min(stock.getReservedQuantity(), quantity);
        int fromAvailable = quantity - fromReserve;

        if (fromAvailable > stock.getQuantity() - stock.getReservedQuantity()) {
            throw new CommonBackendException(
                    String.format("Not enough available stock. Available: %d, Requested: %d",
                            stock.getQuantity() - stock.getReservedQuantity(), fromAvailable),
                    HttpStatus.CONFLICT);
        }

        stock.setReservedQuantity(stock.getReservedQuantity() - fromReserve);
        stock.setQuantity(stock.getQuantity() - quantity);

        log.info("Decreased stock: {} @ {} -{} (from reserve: {}, new total: {}, new reserved: {})",
                productSku, locationCode, quantity, fromReserve, stock.getQuantity(), stock.getReservedQuantity());
    }

    @Transactional(timeout = 5)
    public void transferStock(String productSku, Integer quantity,
                              String fromLocationCode, String toLocationCode) {
        validateTransfer(fromLocationCode, toLocationCode, quantity);

        Product product = productService.getBySku(productSku);
        Location fromLoc = locationService.getByCode(fromLocationCode);
        Location toLoc = locationService.getByCode(toLocationCode);

        boolean lockFromFirst = fromLoc.getId().compareTo(toLoc.getId()) < 0;

        Stock stockFrom, stockTo;
        if (lockFromFirst) {
            stockFrom = findStockWithLock(product, fromLoc);
            stockTo = findOrCreateStockWithLock(product, toLoc);
        } else {
            stockTo = findOrCreateStockWithLock(product, toLoc);
            stockFrom = findStockWithLock(product, fromLoc);
        }

        int availableForTransfer = stockFrom.getQuantity() - stockFrom.getReservedQuantity();
        if (availableForTransfer < quantity) {
            throw new CommonBackendException(
                    String.format("Not enough available stock in %s. Available: %d, Requested: %d",
                            fromLocationCode, availableForTransfer, quantity),
                    HttpStatus.CONFLICT);
        }

        stockFrom.setQuantity(stockFrom.getQuantity() - quantity);
        stockTo.setQuantity(stockTo.getQuantity() + quantity);

        log.info("Transferred {} units of {} from {} to {}",
                quantity, productSku, fromLocationCode, toLocationCode);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reserveStock(String productSku, Integer quantity) {
        validateQuantity(quantity);
        checkAvailability(productSku, quantity);

        Product product = productService.getBySku(productSku);
        List<Stock> stocks = stockRepository.findAvailableByProductIdFifoWithLock(product.getId());

        int remaining = quantity;
        for (Stock stock : stocks) {
            if (remaining <= 0) break;
            int available = stock.getAvailableQuantity();
            if (available > 0) {
                int toReserve = Math.min(available, remaining);
                stock.setReservedQuantity(stock.getReservedQuantity() + toReserve);
                remaining -= toReserve;
            }
        }

        if (remaining > 0) {
            throw new CommonBackendException(
                    String.format("Inconsistent state: failed to reserve full quantity for %s", productSku),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Reserved {} items of {}", quantity, productSku);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Stock findStockWithLock(Product product, Location location) {
        return stockRepository.findByProductAndLocationWithLock(product, location)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("Stock not found at location %s", location.getCode()),
                        HttpStatus.NOT_FOUND));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Stock findOrCreateStockWithLock(Product product, Location location) {
        return stockRepository.findByProductAndLocationWithLock(product, location)
                .orElseGet(() -> createStockWithRetry(product, location));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Stock createStockWithRetry(Product product, Location location) {
        try {
            Stock newStock = new Stock();
            newStock.setProduct(product);
            newStock.setLocation(location);
            newStock.setQuantity(0);
            newStock.setReservedQuantity(0);
            newStock.setInboundDate(Instant.now());
            return stockRepository.saveAndFlush(newStock);
        } catch (DataIntegrityViolationException e) {
            return stockRepository.findByProductAndLocationWithLock(product, location)
                    .orElseThrow(() -> {
                        log.warn("Stock disappeared after race condition at {}", location.getCode());
                        return new CommonBackendException(
                                "Concurrent stock operation failed. Please retry.",
                                HttpStatus.CONFLICT);
                    });
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void checkAvailability(String productSku, Integer requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity <= 0) {
            throw new CommonBackendException("Quantity must be positive", HttpStatus.BAD_REQUEST);
        }

        Product product = productService.getBySku(productSku);
        int totalAvailable = stockRepository.sumAvailableQuantityByProductId(product.getId());
        if (totalAvailable < requiredQuantity) {
            throw new CommonBackendException(
                    String.format("Not enough available stock for product %s. Required: %d, Available: %d",
                            productSku, requiredQuantity, totalAvailable),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void releaseReserved(String productSku, Integer quantity) {
        Product product = productService.getBySku(productSku);

        List<Stock> stocks = stockRepository.findReservedByProductIdFifoWithLock(product.getId());

        int totalReserved = stocks.stream().mapToInt(Stock::getReservedQuantity).sum();
        if (totalReserved < quantity) {
            throw new CommonBackendException(
                    String.format("Not enough reserved stock to release for product %s. Available reserved: %d, Requested: %d",
                            productSku, totalReserved, quantity),
                    HttpStatus.CONFLICT);
        }

        int remaining = quantity;
        for (Stock stock : stocks) {
            if (remaining <= 0) break;
            int reserved = stock.getReservedQuantity();
            if (reserved > 0) {
                int toRelease = Math.min(reserved, remaining);
                stock.setReservedQuantity(stock.getReservedQuantity() - toRelease);
                remaining -= toRelease;
            }
        }

        log.info("Released {} reserved items of {}", quantity, productSku);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CommonBackendException(
                    String.format("Quantity must be positive, got: %s", quantity),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> findFirstAvailableLocationCodeForProduct(String productSku) {
        return stockRepository.findFirstAvailableLocationForProduct(productSku)
                .map(Location::getCode);
    }

    private void validateTransfer(String fromLocationCode, String toLocationCode, Integer quantity) {
        if (fromLocationCode.equals(toLocationCode)) {
            throw new CommonBackendException(
                    String.format("Source and destination locations cannot be same: %s", fromLocationCode),
                    HttpStatus.BAD_REQUEST);
        }
        validateQuantity(quantity);
    }
}