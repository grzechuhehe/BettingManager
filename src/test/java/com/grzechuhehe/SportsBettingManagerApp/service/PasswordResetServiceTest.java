package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.PasswordResetSubmit;
import com.grzechuhehe.SportsBettingManagerApp.model.PasswordResetToken;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.PasswordResetTokenRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("oldEncodedPassword");
    }

    @Test
    void createPasswordResetTokenForUser_ShouldCreateToken_WhenUserExists() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        String token = passwordResetService.createPasswordResetTokenForUser("test@example.com");

        // Then
        assertThat(token).isNotNull();
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    @Test
    void createPasswordResetTokenForUser_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            passwordResetService.createPasswordResetTokenForUser("unknown@example.com")
        );
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_ShouldUpdatePassword_WhenTokenIsValid() {
        // Given
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setUser(testUser);
        // Expiry defaults to +24h in constructor, so it's valid

        PasswordResetSubmit submit = new PasswordResetSubmit();
        submit.setToken(tokenString);
        submit.setNewPassword("newPassword");

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        // When
        passwordResetService.resetPassword(submit);

        // Then
        verify(userRepository).save(testUser);
        verify(tokenRepository).delete(token);
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsExpired() {
        // Given
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setUser(testUser);
        
        // Use reflection to set expiry date to the past
        ReflectionTestUtils.setField(token, "expiryDate", LocalDateTime.now().minusHours(1));

        PasswordResetSubmit submit = new PasswordResetSubmit();
        submit.setToken(tokenString);
        submit.setNewPassword("newPassword");

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            passwordResetService.resetPassword(submit)
        );
        verify(userRepository, never()).save(any());
        // Service deletes token if expired
        verify(tokenRepository).delete(token);
    }
}