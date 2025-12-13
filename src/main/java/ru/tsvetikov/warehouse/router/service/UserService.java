//package ru.tsvetikov.warehouse.router.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import ru.tsvetikov.warehouse.router.exception.CommonBackendException;
//import ru.tsvetikov.warehouse.router.model.db.entity.User;
//import ru.tsvetikov.warehouse.router.model.db.repository.UserRepository;
//import ru.tsvetikov.warehouse.router.model.dto.request.UserRequest;
//import ru.tsvetikov.warehouse.router.model.dto.response.UserResponse;
//import ru.tsvetikov.warehouse.router.model.mapper.UserMapper;
//
//import java.util.List;
//import java.util.stream.Collectors;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class UserService {
//    private final UserRepository userRepository;
//    private final UserMapper userMapper;
//    private final PasswordEncoder passwordEncoder;
//
//    @Transactional
//    public UserResponse create(UserRequest request) {
//        if (userRepository.findByUsername(request.username()).isPresent()) {
//            throw new CommonBackendException(
//                    "User with username already exists: " + request.username(),
//                    HttpStatus.CONFLICT);
//        }
//
//        User user = userMapper.toEntity(request);
//        user.setPasswordHash(passwordEncoder.encode(request.password()));
//        User saved = userRepository.save(user);
//
//        return userMapper.toResponseDto(saved);
//    }
//
//    @Transactional(readOnly = true)
//    public UserResponse getById(Long id) {
//        User user = getUserEntityById(id);
//        return userMapper.toResponseDto(user);
//    }
//
//    @Transactional(readOnly = true)
//    public List<UserResponse> getAll() {
//        return userRepository.findAll().stream()
//                .map(userMapper::toResponseDto)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public UserResponse getByUsername(String username) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new CommonBackendException(
//                        "User not found with username: " + username,
//                        HttpStatus.NOT_FOUND));
//        return userMapper.toResponseDto(user);
//    }
//
//    @Transactional
//    public UserResponse update(Long id, UserRequest request) {
//        User existing = getUserEntityById(id);
//
//
//        if (request.username() != null && !request.username().equals(existing.getUsername())) {
//            if (userRepository.findByUsername(request.username()).isPresent()) {
//                throw new CommonBackendException(
//                        "User with username already exists: " + request.username(),
//                        HttpStatus.CONFLICT);
//            }
//        }
//
//        if (request.password() != null) {
//            existing.setPasswordHash(passwordEncoder.encode(request.password()));
//        }
//
//        userMapper.updateEntityFromDto(request, existing);
//        User updated = userRepository.save(existing);
//
//        return userMapper.toResponseDto(updated);
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        User user = getUserEntityById(id);
//        user.setIsActive(false);
//    }
//
//    @Transactional(readOnly = true)
//    public User getUserEntityById(Long id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new CommonBackendException(
//                        "User not found with id: " + id,
//                        HttpStatus.NOT_FOUND));
//    }
//
//    @Transactional
//    public void updateUserLocation(Long userId, Double x, Double y) {
//        User user = getUserEntityById(userId);
//        user.setLastKnownX(x);
//        user.setLastKnownY(y);
//    }
//}
