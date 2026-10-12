package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.db.repository.UserRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.ChangePasswordRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserCreateRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserUpdateRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
import ru.tsvetikov.warehouse.router.model.enums.Role;
import ru.tsvetikov.warehouse.router.model.mapper.UserMapper;
import ru.tsvetikov.warehouse.router.testdata.UserTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUser() {
        UserCreateRequest request = UserTestData.createRequest();
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();

        when(userRepository.existsByUsernameIgnoreCase("username")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("password")).thenReturn("hash");
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getUsername()).isEqualTo("username");
        verify(userRepository).existsByUsernameIgnoreCase("username");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(entity);
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExist() {
        UserCreateRequest request = UserTestData.createRequest();

        when(userRepository.existsByUsernameIgnoreCase("username")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with username '%s' already exists", "username");

        verifyNoInteractions(userMapper);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
        verify(userRepository).existsByUsernameIgnoreCase("username");
    }

    @Test
    void shouldThrowWhenUsernameIsNull() {
        UserCreateRequest request = UserTestData.createRequest(null);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Username must not be blank");

        verifyNoInteractions(userMapper);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUsernameIsBlank() {
        UserCreateRequest request = UserTestData.createRequest("   ");

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Username must not be blank");

        verifyNoInteractions(userMapper);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFindUserById() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        UserResponse result = userService.getById(id);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(id);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with id: %s not found", id);

        verifyNoInteractions(userMapper);
        verify(userRepository).findById(id);
    }

    @Test
    void shouldReturnPageOfUsers() {
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(userRepository.findAllByIsActiveTrue(any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        Page<UserResponse> result = userService.getAll(0, 10, "username", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(userRepository).findAllByIsActiveTrue(any(Pageable.class));
        verify(userMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnListOfActiveUsers() {
        UserResponse response = UserTestData.response();
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findAllByIsActiveTrue(Sort.by("username"))).thenReturn(List.of(entity));
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        List<UserResponse> result = userService.getAllActiveUsers();

        assertThat(result).isEqualTo(List.of(response));
        verify(userRepository).findAllByIsActiveTrue(Sort.by("username"));
        verify(userMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnPageOfUsersSortedByRole() {
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();
        Role role = Role.PICKER;
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(userRepository.searchActiveByRole(eq(role), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        Page<UserResponse> result = userService.getByRole(role, 0, 10, "username", Sort.Direction.DESC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(userRepository).searchActiveByRole(eq(role), any(Pageable.class));
        verify(userMapper).toResponseDto(entity);
    }

    @Test
    void shouldSearchPageOfUsersByQuery() {
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();
        String query = "query";
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(userRepository.searchActive(eq(query), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        Page<UserResponse> result = userService.search(query, 0, 10, "username", Sort.Direction.ASC);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        verify(userRepository).searchActive(eq(query), any(Pageable.class));
        verify(userMapper).toResponseDto(entity);
    }

    @Test
    void shouldReturnUserEntityByUserName() {
        String username = "username";
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(entity));

        User result = userService.getUserEntityByUserName(username);

        assertThat(result).isEqualTo(entity);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnGetUserEntityByUserName() {
        String username = "username";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEntityByUserName(username))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with username '%s' not found", username);

        verify(userRepository).findByUsername(username);
    }

    @Test
    void shouldUpdateUser() {
        Long id = 1L;
        UserUpdateRequest request = UserTestData.updateRequest();
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        UserResponse result = userService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getUsername()).isEqualTo("username");
        verify(userRepository).findById(id);
        verify(userMapper).updateEntityFromDto(request, entity);
        verify(userRepository).save(entity);
    }

    @Test
    void shouldUpdateUsername() {
        Long id = 1L;
        UserUpdateRequest request = UserTestData.updateRequest("NewUsername");
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.existsByUsernameIgnoreCase("newusername")).thenReturn(false);
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        UserResponse result = userService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getUsername()).isEqualTo("newusername");
        verify(userRepository).findById(id);
        verify(userRepository).existsByUsernameIgnoreCase("newusername");
        verify(userMapper).updateEntityFromDto(request, entity);
        verify(userRepository).save(entity);
    }

    @Test
    void shouldNotUpdateUsernameWhenUsernameIsNull() {
        Long id = 1L;
        UserUpdateRequest request = UserTestData.updateRequest(null);
        User entity = UserTestData.entityBuilder().id(1L).build();
        UserResponse response = UserTestData.response();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponseDto(entity)).thenReturn(response);

        UserResponse result = userService.update(id, request);

        assertThat(result).isEqualTo(response);
        assertThat(entity.getUsername()).isEqualTo("username");
        verify(userRepository).findById(id);
        verify(userMapper).updateEntityFromDto(request, entity);
        verify(userRepository).save(entity);
    }

    @Test
    void shouldThrowWhenUsernameIsBlankOnUpdate() {
        Long id = 1L;
        UserUpdateRequest request = UserTestData.updateRequest("   ");
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> userService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Username must not be blank");

        verifyNoInteractions(userMapper);
        verify(userRepository).findById(id);
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExistOnUpdate() {
        Long id = 1L;
        UserUpdateRequest request = UserTestData.updateRequest("Newusername");
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.existsByUsernameIgnoreCase("newusername")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with username '%s' already exists", "newusername");

        verify(userRepository).findById(id);
        verify(userRepository).existsByUsernameIgnoreCase("newusername");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeleteUser() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);

        userService.delete(id);

        assertThat(entity.getIsActive()).isFalse();
        verify(userRepository).findById(id);
        verify(userRepository).save(entity);
    }

    @Test
    void shouldThrowWhenUserAlreadyDeleted() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).isActive(false).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User is already deleted");

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDelete() {
        Long id = 99L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with id: %s not found", id);

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldActivateUser() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).isActive(false).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);

        userService.activate(id);

        assertThat(entity.getIsActive()).isTrue();
        verify(userRepository).findById(id);
        verify(userRepository).save(entity);
    }

    @Test
    void shouldThrowWhenUserAlreadyActive() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).isActive(true).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> userService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User is already active");

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnActivate() {
        Long id = 99L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activate(id))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with id: %s not found", id);

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldChangePasswordForCurrentUser() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");
        User user = UserTestData.entityBuilder().id(1L).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("username");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");
        when(userRepository.save(user)).thenReturn(user);

        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            userService.changePasswordForCurrentUser(request);

            assertThat(user.getPasswordHash()).isEqualTo("newHash");
            verify(userRepository).findByUsername("username");
            verify(passwordEncoder).matches("oldPassword", "hash");
            verify(passwordEncoder).encode("newPassword");
            verify(userRepository).save(user);
        }
    }

    @Test
    void shouldThrowWhenUserNotFoundOnChangePasswordForCurrentUser() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("username");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            assertThatThrownBy(() -> userService.changePasswordForCurrentUser(request))
                    .isInstanceOf(CommonBackendException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository).findByUsername("username");
            verify(passwordEncoder, never()).matches(any(), anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void shouldChangePasswordByUserId() {
        Long id = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("oldPassword", "hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

        userService.changePassword(id, request);

        assertThat(entity.getPasswordHash()).isEqualTo("newHash");
        verify(userRepository).findById(id);
        verify(passwordEncoder).matches("oldPassword", "hash");
        verify(userRepository).save(entity);
    }

    @Test
    void shouldThrowWhenOldPasswordIncorrect() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).build();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("oldPassword", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("Старый пароль неверен");

        verify(userRepository).findById(id);
        verify(passwordEncoder).matches("oldPassword", "hash");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnChangePassword() {
        Long id = 99L;
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(id, request))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with id: %s not found", id);

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdateUserLocation() {
        Long id = 1L;
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));

        userService.updateLocation(id, 10.0, 20.0);

        assertThat(entity.getLastKnownX()).isEqualTo(10.0);
        assertThat(entity.getLastKnownY()).isEqualTo(20.0);
        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnUpdateUserLocation() {
        Long id = 99L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateLocation(id, 10.0, 20.0))
                .isInstanceOf(CommonBackendException.class)
                .hasMessageContaining("User with id: %s not found", id);

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }
}