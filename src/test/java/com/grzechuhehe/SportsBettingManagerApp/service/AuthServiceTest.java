package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.SignupRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUser_ShouldSaveUser_WhenRequestIsValid() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("newUser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L); // Simulate DB ID generation
            return savedUser;
        });

        // When
        User result = authService.registerUser(request);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("newUser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getRoles()).containsExactly("USER");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registerUser_ShouldThrowException_WhenUsernameTaken() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("existingUser");

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            authService.registerUser(request)
        );

        assertThat(exception.getMessage()).contains("Username is already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailTaken() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("newUser");
        request.setEmail("existing@example.com");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            authService.registerUser(request)
        );

        assertThat(exception.getMessage()).contains("Email is already in use");
        verify(userRepository, never()).save(any());
    }
}
