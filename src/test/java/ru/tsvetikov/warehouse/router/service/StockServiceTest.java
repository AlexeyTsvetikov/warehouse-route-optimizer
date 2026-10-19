package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.entity.Product;
import ru.tsvetikov.warehouse.router.model.db.entity.Stock;
import ru.tsvetikov.warehouse.router.model.db.repository.StockRepository;
import ru.tsvetikov.warehouse.router.model.dto.response.StockResponse;
import ru.tsvetikov.warehouse.router.model.mapper.StockMapper;
import ru.tsvetikov.warehouse.router.testdata.LocationTestData;
import ru.tsvetikov.warehouse.router.testdata.ProductTestData;
import ru.tsvetikov.warehouse.router.testdata.StockTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock

    private StockMapper stockMapper;
    @Mock

    private ProductService productService;
    @Mock

    private LocationService locationService;

    @InjectMocks
    private StockService stockService;

    @Test
    void shouldReturnStockByProductId() {
        Long productId = 1L;
        Stock stock = StockTestData.entityBuilder().id(1L).build();
        StockResponse response = StockTestData.response();

        when(stockRepository.findByProductId(productId)).thenReturn(List.of(stock));
        when(stockMapper.toResponse(stock)).thenReturn(response);

        List<StockResponse> result = stockService.getByProduct(productId);

        assertThat(result).containsExactly(response);
        verify(stockRepository).findByProductId(productId);
    }

    @Test
    void shouldReturnPageOfStocksByLocationCodeAndQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Stock stock = StockTestData.entityBuilder().id(1L).build();
        StockResponse response = StockTestData.response();
        String locationCode = "PICK-01";
        String query = "query";
        Page<Stock> page = new PageImpl<>(List.of(stock), pageable, 1);

        when(stockRepository.findFiltered(eq(locationCode), eq(query), any(Pageable.class))).thenReturn(page);
        when(stockMapper.toResponse(stock)).thenReturn(response);

        Page<StockResponse> result = stockService.getFiltered(locationCode, query, 1, 10);

        assertThat(result.getContent()).containsExactly(response);
        verify(stockRepository).findFiltered(eq(locationCode), eq(query), any(Pageable.class));
    }

    @Test
    void shouldUseEmptyStringsWhenFiltersAreNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Stock stock = StockTestData.entityBuilder().id(1L).build();
        StockResponse response = StockTestData.response();
        Page<Stock> page = new PageImpl<>(List.of(stock), pageable, 1);

        when(stockRepository.findFiltered(eq(""), eq(""), any(Pageable.class))).thenReturn(page);
        when(stockMapper.toResponse(stock)).thenReturn(response);

        Page<StockResponse> result = stockService.getFiltered(null, null, 1, 10);

        assertThat(result.getContent()).containsExactly(response);
        verify(stockRepository).findFiltered(eq(""), eq(""), any(Pageable.class));
    }

    @Test
    void shouldIncreaseStock() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 100;
        Stock stock = StockTestData.entityBuilder().id(1L).build();
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.of(stock));

        stockService.increaseStock(productSku, locationCode, quantity);

        assertThat(stock.getQuantity()).isEqualTo(200);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, location);
    }

    @Test
    void shouldThrowWhenQuantityIsNull() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = null;

        assertThatThrownBy(() -> stockService.increaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive, got: %s", quantity);

        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldThrowWhenQuantityIsNegative() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = -10;

        assertThatThrownBy(() -> stockService.increaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive, got: %s", quantity);

        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldCreateNewStockEntityWhenStockNotFound() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.empty());
        when(stockRepository.saveAndFlush(any(Stock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        stockService.increaseStock(productSku, locationCode, quantity);

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).saveAndFlush(captor.capture());

        Stock saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(100);
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getLocation()).isEqualTo(location);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, location);
        verify(stockRepository).saveAndFlush(any(Stock.class));
    }

    @Test
    void shouldThrowWhenConcurrentStockCreationFails() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.empty());
        when(stockRepository.saveAndFlush(any(Stock.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> stockService.increaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Concurrent stock operation failed. Please retry.");

        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository, times(2)).findByProductAndLocationWithLock(product, location);
        verify(stockRepository).saveAndFlush(any(Stock.class));
    }

    @Test
    void shouldReturnExistingStockWhenCreationFails() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock existingStock = StockTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingStock));
        when(stockRepository.saveAndFlush(any(Stock.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        stockService.increaseStock(productSku, locationCode, quantity);

        assertThat(existingStock.getQuantity()).isEqualTo(200);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository, times(2)).findByProductAndLocationWithLock(product, location);
        verify(stockRepository).saveAndFlush(any(Stock.class));
    }

    @Test
    void shouldDecreaseStock() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock = StockTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.of(stock));

        stockService.decreaseStock(productSku, locationCode, quantity);

        assertThat(stock.getQuantity()).isEqualTo(0);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, location);
    }

    @Test
    void shouldDecreaseStockFromReservedFirst() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 70;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock = StockTestData.entityBuilder().id(1L).reservedQuantity(50).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.of(stock));

        stockService.decreaseStock(productSku, locationCode, quantity);

        assertThat(stock.getQuantity()).isEqualTo(30);
        assertThat(stock.getReservedQuantity()).isZero();
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, location);
    }

    @Test
    void shouldThrowWhenStockNotFound() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.decreaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Stock not found for product %s at location %s", productSku, locationCode);

        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
    }

    @Test
    void shouldThrowWhenQuantityIsNullOnDecreaseStock() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = null;

        assertThatThrownBy(() -> stockService.decreaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive, got: %s", quantity);

        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldThrowWhenQuantityIsNegativeOnDecrease() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = -10;

        assertThatThrownBy(() -> stockService.decreaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive, got: %s", quantity);

        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldThrowWhenNotEnoughAvailableStock() {
        String productSku = "BRP-ENG-001";
        String locationCode = "PICK-01";
        Integer quantity = 150;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock = StockTestData.entityBuilder().id(1L).reservedQuantity(50).build();
        Location location = LocationTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(locationCode)).thenReturn(location);
        when(stockRepository.findByProductAndLocationWithLock(product, location)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> stockService.decreaseStock(productSku, locationCode, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Not enough available stock.");

        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(locationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, location);
    }

    @Test
    void shouldTransferStock() {
        String productSku = "BRP-ENG-001";
        String fromLocationCode = "A";
        String toLocationCode = "B";
        Integer quantity = 50;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location locA = LocationTestData.entityBuilder().id(1L).code(fromLocationCode).build();
        Location locB = LocationTestData.entityBuilder().id(2L).code(toLocationCode).build();
        Stock stockA = StockTestData.entityBuilder().id(1L).location(locA).quantity(100).reservedQuantity(0).build();
        Stock stockB = StockTestData.entityBuilder().id(2L).location(locB).quantity(0).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(fromLocationCode)).thenReturn(locA);
        when(locationService.getByCode(toLocationCode)).thenReturn(locB);
        when(stockRepository.findByProductAndLocationWithLock(product, locA)).thenReturn(Optional.of(stockA));
        when(stockRepository.findByProductAndLocationWithLock(product, locB)).thenReturn(Optional.of(stockB));

        stockService.transferStock(productSku, quantity, fromLocationCode, toLocationCode);

        assertThat(stockA.getQuantity()).isEqualTo(50);
        assertThat(stockB.getQuantity()).isEqualTo(50);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(fromLocationCode);
        verify(locationService).getByCode(toLocationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, locA);
        verify(stockRepository).findByProductAndLocationWithLock(product, locB);
    }

    @Test
    void shouldThrowWhenNotEnoughAvailableStockOnTransfer() {
        String productSku = "BRP-ENG-001";
        String fromLocationCode = "A";
        String toLocationCode = "B";
        Integer quantity = 150;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location locA = LocationTestData.entityBuilder().id(1L).code(fromLocationCode).build();
        Location locB = LocationTestData.entityBuilder().id(2L).code(toLocationCode).build();
        Stock stockA = StockTestData.entityBuilder().id(1L).location(locA).quantity(100).reservedQuantity(0).build();
        Stock stockB = StockTestData.entityBuilder().id(2L).location(locB).quantity(0).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(fromLocationCode)).thenReturn(locA);
        when(locationService.getByCode(toLocationCode)).thenReturn(locB);
        when(stockRepository.findByProductAndLocationWithLock(product, locA)).thenReturn(Optional.of(stockA));
        when(stockRepository.findByProductAndLocationWithLock(product, locB)).thenReturn(Optional.of(stockB));


        assertThatThrownBy(() -> stockService.transferStock(productSku, quantity, fromLocationCode, toLocationCode))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Not enough available stock in %s. Available: %d, Requested: %d",
                        fromLocationCode, stockA.getQuantity() - stockA.getReservedQuantity(), quantity);

        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(fromLocationCode);
        verify(locationService).getByCode(toLocationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, locA);
        verify(stockRepository).findByProductAndLocationWithLock(product, locB);
    }

    @Test
    void shouldThrowWhenFromLocationIsTheSameAsTargetLocation() {
        String productSku = "BRP-ENG-001";
        String fromLocationCode = "A";
        Integer quantity = 100;

        assertThatThrownBy(() -> stockService.transferStock(productSku, quantity, fromLocationCode, fromLocationCode))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Source and destination locations cannot be same: %s", fromLocationCode);

        verifyNoInteractions(productService);
        verifyNoInteractions(locationService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldTransferStockWhenFromLocationHasGreaterId() {
        String productSku = "BRP-ENG-001";
        String fromLocationCode = "A";
        String toLocationCode = "B";
        Integer quantity = 50;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location locA = LocationTestData.entityBuilder().id(2L).code(fromLocationCode).build();
        Location locB = LocationTestData.entityBuilder().id(1L).code(toLocationCode).build();
        Stock stockA = StockTestData.entityBuilder().id(1L).location(locA).quantity(100).reservedQuantity(0).build();
        Stock stockB = StockTestData.entityBuilder().id(2L).location(locB).quantity(0).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(fromLocationCode)).thenReturn(locA);
        when(locationService.getByCode(toLocationCode)).thenReturn(locB);
        when(stockRepository.findByProductAndLocationWithLock(product, locA)).thenReturn(Optional.of(stockA));
        when(stockRepository.findByProductAndLocationWithLock(product, locB)).thenReturn(Optional.of(stockB));

        stockService.transferStock(productSku, quantity, fromLocationCode, toLocationCode);

        assertThat(stockA.getQuantity()).isEqualTo(50);
        assertThat(stockB.getQuantity()).isEqualTo(50);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(fromLocationCode);
        verify(locationService).getByCode(toLocationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, locA);
        verify(stockRepository).findByProductAndLocationWithLock(product, locB);
    }

    @Test
    void shouldThrowWhenStockNotFoundAtLocation() {
        String productSku = "BRP-ENG-001";
        String fromLocationCode = "A";
        String toLocationCode = "B";
        Integer quantity = 50;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location locA = LocationTestData.entityBuilder().id(1L).code(fromLocationCode).build();
        Location locB = LocationTestData.entityBuilder().id(2L).code(toLocationCode).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(fromLocationCode)).thenReturn(locA);
        when(locationService.getByCode(toLocationCode)).thenReturn(locB);
        when(stockRepository.findByProductAndLocationWithLock(product, locA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.transferStock(productSku, quantity, fromLocationCode, toLocationCode))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Stock not found at location %s", fromLocationCode);

        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(fromLocationCode);
        verify(locationService).getByCode(toLocationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, locA);
        verify(stockRepository, never()).findByProductAndLocationWithLock(product, locB);
    }

    @Test
    void shouldCreateNewStockEntityWhenStockOnDestLocationNotFound() {
        String productSku = "BRP-ENG-001";
        String fromLocationCode = "A";
        String toLocationCode = "B";
        Integer quantity = 50;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Location locA = LocationTestData.entityBuilder().id(1L).code(fromLocationCode).build();
        Location locB = LocationTestData.entityBuilder().id(2L).code(toLocationCode).build();
        Stock stockA = StockTestData.entityBuilder().id(1L).location(locA).quantity(100).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(locationService.getByCode(fromLocationCode)).thenReturn(locA);
        when(locationService.getByCode(toLocationCode)).thenReturn(locB);
        when(stockRepository.findByProductAndLocationWithLock(product, locA)).thenReturn(Optional.of(stockA));
        when(stockRepository.findByProductAndLocationWithLock(product, locB)).thenReturn(Optional.empty());
        when(stockRepository.saveAndFlush(any(Stock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        stockService.transferStock(productSku, quantity, fromLocationCode, toLocationCode);

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).saveAndFlush(captor.capture());

        Stock saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(50);
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getLocation()).isEqualTo(locB);
        assertThat(stockA.getQuantity()).isEqualTo(50);
        verify(productService).getBySku(productSku);
        verify(locationService).getByCode(fromLocationCode);
        verify(locationService).getByCode(toLocationCode);
        verify(stockRepository).findByProductAndLocationWithLock(product, locA);
        verify(stockRepository).findByProductAndLocationWithLock(product, locB);
    }

    @Test
    void shouldReserveStockNearest() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 50;
        double fromX = 0.0;
        double fromY = 0.0;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock = StockTestData.entityBuilder().id(1L).quantity(100).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(100);
        when(stockRepository.findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY)).thenReturn(List.of(stock));

        stockService.reserveStockNearest(productSku, quantity, fromX, fromY);

        assertThat(stock.getReservedQuantity()).isEqualTo(50);
        verify(stockRepository).findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
    }

    @Test
    void shouldReserveFromMultipleStocksWhenFirstIsNotEnough() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 50;
        double fromX = 0.0;
        double fromY = 0.0;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).quantity(30).reservedQuantity(0).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).quantity(40).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(70);
        when(stockRepository.findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY)).thenReturn(List.of(stock1, stock2));

        stockService.reserveStockNearest(productSku, quantity, fromX, fromY);

        assertThat(stock1.getReservedQuantity()).isEqualTo(30);
        assertThat(stock2.getReservedQuantity()).isEqualTo(20);
        verify(stockRepository).findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
    }

    @Test
    void shouldThrowWhenStockQuantityIsNotEnough() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 100;
        double fromX = 0.0;
        double fromY = 0.0;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        int totalAvailable = 70;

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(totalAvailable);

        assertThatThrownBy(() -> stockService.reserveStockNearest(productSku, quantity, fromX, fromY))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Not enough available stock for product %s. Required: %d, Available: %d",
                        productSku, quantity, totalAvailable);

        verify(productService).getBySku(productSku);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
    }

    @Test
    void shouldStopReservationWhenQuantityFullyCovered() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 50;
        double fromX = 0.0;
        double fromY = 0.0;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).quantity(50).reservedQuantity(0).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).quantity(40).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(90);
        when(stockRepository.findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY)).thenReturn(List.of(stock1, stock2));

        stockService.reserveStockNearest(productSku, quantity, fromX, fromY);

        assertThat(stock1.getReservedQuantity()).isEqualTo(50);
        assertThat(stock2.getReservedQuantity()).isEqualTo(0);
        verify(stockRepository).findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
    }

    @Test
    void shouldSkipStockWithNoAvailableQuantity() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 50;
        double fromX = 0.0;
        double fromY = 0.0;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).quantity(0).reservedQuantity(0).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).quantity(90).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(90);
        when(stockRepository.findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY)).thenReturn(List.of(stock1, stock2));

        stockService.reserveStockNearest(productSku, quantity, fromX, fromY);

        assertThat(stock1.getReservedQuantity()).isEqualTo(0);
        assertThat(stock2.getReservedQuantity()).isEqualTo(50);
        verify(stockRepository).findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
    }

    @Test
    void shouldThrowWhenNotAllQuantityReservedDueToInconsistentState() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 50;
        double fromX = 0.0;
        double fromY = 0.0;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).quantity(30).reservedQuantity(0).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(100);
        when(stockRepository.findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY))
                .thenReturn(List.of(stock1));

        assertThatThrownBy(() -> stockService.reserveStockNearest(productSku, quantity, fromX, fromY))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Inconsistent state: failed to reserve full quantity");

        assertThat(stock1.getReservedQuantity()).isEqualTo(30);
        verify(productService, times(2)).getBySku(productSku);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
        verify(stockRepository).findAvailableByProductIdNearestFirst(product.getId(), fromX, fromY);
    }

    @Test
    void shouldThrowWhenQuantityIsNullOnCheckAvailability() {
        String productSku = "BRP-ENG-001";

        assertThatThrownBy(() -> stockService.checkAvailability(productSku, null))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive");

        verifyNoInteractions(productService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldThrowWhenQuantityIsNegativeOnCheckAvailability() {
        String productSku = "BRP-ENG-001";
        Integer requiredQuantity = -10;

        assertThatThrownBy(() -> stockService.checkAvailability(productSku, requiredQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Quantity must be positive");

        verifyNoInteractions(productService);
        verifyNoInteractions(stockRepository);
    }

    @Test
    void shouldCheckAvailabilitySuccessfully() {
        String productSku = "BRP-ENG-001";
        Integer requiredQuantity = 50;
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(100);

        stockService.checkAvailability(productSku, requiredQuantity);

        verify(productService).getBySku(productSku);
        verify(stockRepository).sumAvailableQuantityByProductId(product.getId());
    }

    @Test
    void shouldThrowWhenNotEnoughAvailableStockOnCheckAvailability() {
        String productSku = "BRP-ENG-001";
        Integer requiredQuantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.sumAvailableQuantityByProductId(product.getId())).thenReturn(50);

        assertThatThrownBy(() -> stockService.checkAvailability(productSku, requiredQuantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Not enough available stock");
    }

    @Test
    void shouldReleaseReserved() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 50;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).reservedQuantity(30).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).reservedQuantity(40).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.findReservedByProductIdFifoWithLock(product.getId())).thenReturn(List.of(stock1, stock2));

        stockService.releaseReserved(productSku, quantity);

        assertThat(stock1.getReservedQuantity()).isEqualTo(0);
        assertThat(stock2.getReservedQuantity()).isEqualTo(20);
        verify(productService).getBySku(productSku);
        verify(stockRepository).findReservedByProductIdFifoWithLock(product.getId());
    }

    @Test
    void shouldThrowWhenReservedQuantityIsNotEnough() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 100;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).reservedQuantity(30).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).reservedQuantity(40).build();
        int totalReserved = 70;

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.findReservedByProductIdFifoWithLock(product.getId())).thenReturn(List.of(stock1, stock2));

        assertThatThrownBy(() -> stockService.releaseReserved(productSku, quantity))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Not enough reserved stock to release for product %s. Available reserved: %d, Requested: %d",
                        productSku, totalReserved, quantity);

        verify(productService).getBySku(productSku);
        verify(stockRepository).findReservedByProductIdFifoWithLock(product.getId());
    }

    @Test
    void shouldStopReleasingWhenQuantityFullyReleased() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 30;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).reservedQuantity(30).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).reservedQuantity(40).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.findReservedByProductIdFifoWithLock(product.getId())).thenReturn(List.of(stock1, stock2));

        stockService.releaseReserved(productSku, quantity);

        assertThat(stock1.getReservedQuantity()).isEqualTo(0);
        assertThat(stock2.getReservedQuantity()).isEqualTo(40);
        verify(productService).getBySku(productSku);
        verify(stockRepository).findReservedByProductIdFifoWithLock(product.getId());
    }

    @Test
    void shouldSkipStockWithZeroReservedQuantity() {
        String productSku = "BRP-ENG-001";
        Integer quantity = 30;
        Product product = ProductTestData.entityBuilder().id(1L).build();
        Stock stock1 = StockTestData.entityBuilder().id(1L).reservedQuantity(0).build();
        Stock stock2 = StockTestData.entityBuilder().id(2L).reservedQuantity(40).build();

        when(productService.getBySku(productSku)).thenReturn(product);
        when(stockRepository.findReservedByProductIdFifoWithLock(product.getId())).thenReturn(List.of(stock1, stock2));

        stockService.releaseReserved(productSku, quantity);

        assertThat(stock1.getReservedQuantity()).isEqualTo(0);
        assertThat(stock2.getReservedQuantity()).isEqualTo(10);
        verify(productService).getBySku(productSku);
        verify(stockRepository).findReservedByProductIdFifoWithLock(product.getId());
    }

    @Test
    void shouldReturnAvailableStocksByProductSku() {
        String productSku = "BRP-ENG-001";
        List<Stock> stocks = List.of(StockTestData.entityBuilder().id(1L).build());

        when(stockRepository.findAvailableByProductSku(productSku)).thenReturn(stocks);

        List<Stock> result = stockService.findAvailableStocksByProductSku(productSku);

        assertThat(result).isEqualTo(stocks);
        verify(stockRepository).findAvailableByProductSku(productSku);
    }
}