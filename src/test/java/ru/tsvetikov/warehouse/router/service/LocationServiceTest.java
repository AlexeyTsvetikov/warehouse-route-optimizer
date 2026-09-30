package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.Location;
import ru.tsvetikov.warehouse.router.model.db.repository.LocationRepository;
import ru.tsvetikov.warehouse.router.model.dto.form.LocationForm;
import ru.tsvetikov.warehouse.router.model.dto.request.LocationRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.LocationResponse;
import ru.tsvetikov.warehouse.router.model.enums.LocationType;
import ru.tsvetikov.warehouse.router.model.mapper.LocationMapper;
import ru.tsvetikov.warehouse.router.testdata.LocationTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    @Test
    void shouldCreateLocation() {
        LocationRequest request = LocationTestData.request();
        Location entityFromMapper = LocationTestData.entityBuilder().build();
        Location savedEntity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();
        LocationResponse expectedResponse = LocationTestData.response();

        when(locationMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(locationRepository.existsByCodeIgnoreCase("RECV-01")).thenReturn(false);
        when(locationRepository.save(entityFromMapper)).thenReturn(savedEntity);
        when(locationMapper.toResponseDto(savedEntity)).thenReturn(expectedResponse);

        LocationResponse result = locationService.create(request);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(entityFromMapper.getCode()).isEqualTo("RECV-01");

        verify(locationRepository).existsByCodeIgnoreCase("RECV-01");
        verify(locationRepository).save(entityFromMapper);
    }

    @Test
    void shouldThrowWhenLocationCodeExists() {
        LocationRequest request = LocationTestData.request();

        when(locationRepository.existsByCodeIgnoreCase("RECV-01")).thenReturn(true);

        assertThatThrownBy(() -> locationService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with code already exists");

        verify(locationRepository).existsByCodeIgnoreCase("RECV-01");
        verify(locationRepository, never()).save(any());
        verifyNoInteractions(locationMapper);
    }

    @Test
    void shouldThrowWhenCodeIsNull() {
        LocationRequest request = LocationTestData.request(null);

        assertThatThrownBy(() -> locationService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Code must not be blank");

        verifyNoInteractions(locationMapper);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCodeIsBlank() {
        LocationRequest request = LocationTestData.request("   ");

        assertThatThrownBy(() -> locationService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Code must not be blank");

        verifyNoInteractions(locationMapper);
        verify(locationRepository, never()).save(any());
    }


    @Test
    void shouldReturnLocationByCode() {
        String code = "recv-01";
        Location entity = LocationTestData.entityBuilder().code("RECV-01").build();

        when(locationRepository.findByCode(code)).thenReturn(Optional.of(entity));

        Location result = locationService.getByCode(code);

        assertThat(result).isEqualTo(entity);
        verify(locationRepository).findByCode(code);
    }

    @Test
    void shouldThrowWhenLocationNotFoundByCode() {
        String code = "recv-01";

        when(locationRepository.findByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getByCode(code))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with code '%s' not found", code);

        verify(locationRepository).findByCode("recv-01");
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldReturnLocationById() {
        Long id = 1L;
        Location entity = LocationTestData.entityBuilder().id(1L).build();
        LocationResponse response = LocationTestData.response();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        LocationResponse result = locationService.getById(id);

        assertThat(result).isEqualTo(response);
        assertThat(response.id()).isEqualTo(entity.getId());

        verify(locationRepository).findById(id);
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowWhenLocationNotFoundOnGetById() {
        Long id = 1L;

        when(locationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getById(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with id: %s not found", id);

        verifyNoInteractions(locationMapper);
        verify(locationRepository).findById(id);
        verify(locationRepository, never()).save(any());

    }

    @Test
    void shouldUpdateLocationWithoutChangingCodeFromRequest() {
        Long id = 1L;
        LocationRequest request = LocationTestData.request("recv-01");
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();
        LocationResponse response = LocationTestData.response();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.save(entity)).thenReturn(entity);
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        LocationResponse result = locationService.update(id, request);

        assertThat(result).isEqualTo(response);
        verify(locationRepository, never()).existsByCodeIgnoreCase(any());
        verify(locationMapper).updateEntityFromDto(request, entity);
        verify(locationRepository).save(entity);
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldUpdateLocationWithChangingCodeFromRequest() {
        Long id = 1L;
        LocationRequest request = LocationTestData.request("recv-02");
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();
        LocationResponse response = LocationTestData.response();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.existsByCodeIgnoreCase("RECV-02")).thenReturn(false);
        when(locationRepository.save(entity)).thenReturn(entity);
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        LocationResponse result = locationService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getCode()).isEqualTo("RECV-02");
        verify(locationRepository).existsByCodeIgnoreCase("RECV-02");
        verify(locationMapper).updateEntityFromDto(request, entity);
        verify(locationRepository).save(entity);
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldNotChangeCodeWhenNullOnUpdateFromRequest() {
        Long id = 1L;
        LocationRequest request = LocationTestData.request(null);
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();
        LocationResponse response = LocationTestData.response();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.save(entity)).thenReturn(entity);
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        LocationResponse result = locationService.update(id, request);

        assertThat(result).isEqualTo(response);
        verify(locationRepository, never()).existsByCodeIgnoreCase(any());
        verify(locationMapper).updateEntityFromDto(request, entity);
        verify(locationRepository).save(entity);
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowWhenLocationCodeExistsOnUpdateFromRequest() {
        Long id = 1L;
        LocationRequest request = LocationTestData.request("recv-02");
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.existsByCodeIgnoreCase("RECV-02")).thenReturn(true);

        assertThatThrownBy(() -> locationService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with code already exists");

        verify(locationRepository).findById(id);
        verify(locationRepository).existsByCodeIgnoreCase("RECV-02");
        verify(locationRepository, never()).save(any());
        verifyNoInteractions(locationMapper);
    }


    @Test
    void shouldUpdateLocationWithoutChangingCodeFromWeb() {
        Long id = 1L;
        LocationForm form = LocationTestData.form("recv-01");
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.save(entity)).thenReturn(entity);

        locationService.updateFromWeb(id, form);

        verify(locationRepository, never()).existsByCodeIgnoreCase(any());
        verify(locationMapper).updateEntityFromForm(form, entity);
        verify(locationRepository).save(entity);
    }

    @Test
    void shouldUpdateLocationWithChangingCodeFromWeb() {
        Long id = 1L;
        LocationForm form = LocationTestData.form("recv-02");
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.existsByCodeIgnoreCase("RECV-02")).thenReturn(false);
        when(locationRepository.save(entity)).thenReturn(entity);

        locationService.updateFromWeb(id, form);

        assertThat(entity.getCode()).isEqualTo("RECV-02");
        verify(locationRepository).existsByCodeIgnoreCase("RECV-02");
        verify(locationMapper).updateEntityFromForm(form, entity);
        verify(locationRepository).save(entity);
    }

    @Test
    void shouldThrowWhenLocationCodeExistsOnUpdateFromWeb() {
        Long id = 1L;
        LocationForm form = LocationTestData.form("recv-02");
        Location entity = LocationTestData.entityBuilder().id(1L).code("RECV-01").build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.existsByCodeIgnoreCase("RECV-02")).thenReturn(true);

        assertThatThrownBy(() -> locationService.updateFromWeb(id, form))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with code already exists");

        verify(locationRepository).findById(id);
        verify(locationRepository).existsByCodeIgnoreCase("RECV-02");
        verify(locationRepository, never()).save(any());
        verifyNoInteractions(locationMapper);
    }

    @Test
    void shouldSearchLocations() {
        String query = "recv";
        Pageable pageable = PageRequest.of(0, 10);

        Location entity = LocationTestData.entityBuilder().id(1L).build();
        LocationResponse response = LocationTestData.response();

        Page<Location> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(locationRepository.searchActive(eq(query), any(Pageable.class))).thenReturn(page);
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        Page<LocationResponse> result = locationService.search(query, 0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(locationRepository).searchActive(eq(query), any(Pageable.class));
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnPageOfLocations() {
        Pageable pageable = PageRequest.of(0, 10);

        Location entity = LocationTestData.entityBuilder().id(1L).build();
        LocationResponse response = LocationTestData.response();

        Page<Location> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(locationRepository.findAllByIsActiveTrue(any(Pageable.class))).thenReturn(page);
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        Page<LocationResponse> result = locationService.getAll(0, 10, "name", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(locationRepository).findAllByIsActiveTrue(any(Pageable.class));
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldDeleteLocationById() {
        Long id = 1L;
        Location entity = LocationTestData.entityBuilder().id(1L).build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.save(entity)).thenReturn(entity);

        locationService.delete(id);

        assertThat(entity.getIsActive()).isFalse();
        verify(locationRepository).findById(id);
        verify(locationRepository).save(entity);
    }

    @Test
    void shouldThrowWhenLocationNotFoundOnDelete() {
        Long id = 99L;

        when(locationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with id: %s not found", id);

        verify(locationRepository).findById(id);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenLocationAlreadyDeleted() {
        Long id = 1L;
        Location entity = LocationTestData.entityBuilder().id(1L).isActive(false).build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> locationService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location is already deleted");

        verify(locationRepository).findById(id);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldActivateLocationById() {
        Long id = 1L;
        Location entity = LocationTestData.entityBuilder().id(1L).isActive(false).build();
        LocationResponse response = LocationTestData.response();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(locationRepository.save(entity)).thenReturn(entity);
        when(locationMapper.toResponseDto(entity)).thenReturn(response);

        LocationResponse result = locationService.activate(id);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getIsActive()).isTrue();
        verify(locationRepository).findById(id);
        verify(locationRepository).save(entity);
        verify(locationMapper).toResponseDto(entity);
    }

    @Test
    void shouldThrowWhenLocationAlreadyActive() {
        Long id = 1L;
        Location entity = LocationTestData.entityBuilder().id(1L).isActive(true).build();

        when(locationRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> locationService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location is already active");

        verifyNoInteractions(locationMapper);
        verify(locationRepository).findById(id);
        verify(locationRepository, never()).save(any());

    }

    @Test
    void shouldThrowWhenLocationNotFoundOnActivate() {
        Long id = 99L;

        when(locationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Location with id: %s not found", id);

        verifyNoInteractions(locationMapper);
        verify(locationRepository).findById(id);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldReturnLocationsByType() {
        LocationType type = LocationType.RECEIVING;
        Location entity1 = LocationTestData.entityBuilder().code("RECV-01").build();
        Location entity2 = LocationTestData.entityBuilder().code("RECV-02").build();

        when(locationRepository.findByType(type)).thenReturn(List.of(entity1, entity2));

        List<Location> result = locationService.findByType(type);

        assertThat(result).containsExactly(entity1, entity2);
        verify(locationRepository).findByType(type);
    }

    @Test
    void shouldReturnEmptyListWhenNoLocationsByType() {
        LocationType type = LocationType.RECEIVING;

        when(locationRepository.findByType(type)).thenReturn(List.of());

        List<Location> result = locationService.findByType(type);

        assertThat(result).isEmpty();
        verify(locationRepository).findByType(type);
    }
}