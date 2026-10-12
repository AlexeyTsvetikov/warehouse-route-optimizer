package ru.tsvetikov.warehouse.router.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.tsvetikov.warehouse.router.model.db.entity.User;
import ru.tsvetikov.warehouse.router.model.db.repository.UserRepository;
import ru.tsvetikov.warehouse.router.testdata.UserTestData;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldLoadUserByUsername() {
        String username = "username";
        User entity = UserTestData.entityBuilder().id(1L).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(entity));

        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        assertThat(result.getUsername()).isEqualTo(entity.getUsername());
        assertThat(result.getPassword()).isEqualTo(entity.getPasswordHash());
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PICKER");

        verify(userRepository).findByUsername(username);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        String username = "unknown";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + username);

        verify(userRepository).findByUsername(username);
    }
}