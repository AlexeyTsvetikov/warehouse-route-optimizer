package ru.tsvetikov.warehouse.router.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.db.repository.UserRepository;
import ru.tsvetikov.warehouse.router.model.dto.request.ChangePasswordRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserCreateRequest;
import ru.tsvetikov.warehouse.router.model.dto.request.UserUpdateRequest;
import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
import ru.tsvetikov.warehouse.router.model.enums.Role;
import ru.tsvetikov.warehouse.router.model.mapper.UserMapper;
import ru.tsvetikov.warehouse.router.utils.PaginationUtils;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String formattedUsername = formatUsername(request.username());
        checkUsernameUniqueness(formattedUsername);

        User user = userMapper.toEntity(request);
        user.setUsername(formattedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        return userMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponseDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Integer page, Integer perPage,
                                     String sort, Sort.Direction order) {
        Pageable pageRequest = PaginationUtils.getPageRequest(page, perPage, sort, order);
        return userRepository.findAllByIsActiveTrue(pageRequest)
                .map(userMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getByRole(Role role, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<User> users = userRepository.searchActiveByRole(role, pageable);
        return users.map(userMapper::toResponseDto);
    }


    @Transactional(readOnly = true)
    public Page<UserResponse> search(String query, int page, int size, String sort, Sort.Direction order) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(order, sort));
        Page<User> users = userRepository.searchActive(query, pageable);
        return users.map(userMapper::toResponseDto);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUserOrThrow(id);

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            String formattedUsername = formatUsername(request.username());
            checkUsernameUniqueness(formattedUsername);
            user.setUsername(formattedUsername);
        }

        userMapper.updateEntityFromDto(request, user);

        User updated = userRepository.save(user);
        return userMapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        User user = findUserOrThrow(id);

        if (!user.getIsActive()) {
            throw new CommonBackendException("User is already deleted", HttpStatus.CONFLICT);
        }

        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse activate(Long id) {
        User user = findUserOrThrow(id);

        if (user.getIsActive()) {
            throw new CommonBackendException("User is already active", HttpStatus.CONFLICT);
        }

        user.setIsActive(true);
        return userMapper.toResponseDto(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new CommonBackendException("Старый пароль неверен", HttpStatus.CONFLICT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void updateLocation(Long id, Double x, Double y) {
        User user = findUserOrThrow(id);
        user.setLastKnownX(x);
        user.setLastKnownY(y);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CommonBackendException(
                        String.format("User with id: %s not found", id), HttpStatus.NOT_FOUND));
    }

    private String formatUsername(String username) {
        if (username == null || username.isBlank()) return username;
        return username.toLowerCase();
    }

    private void checkUsernameUniqueness(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new CommonBackendException(
                    String.format("User with username '%s' already exists", username), HttpStatus.CONFLICT);
        }
    }
}