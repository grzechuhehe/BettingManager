package com.grzechuhehe.SportsBettingManagerApp.util;

import com.grzechuhehe.SportsBettingManagerApp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String jwtSecret = "testSecretKeyWhichMustBeLongEnoughToMeetHS256Requirements1234567890"; // >= 256 bits
    private final int jwtExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", jwtExpirationMs);
    }

    @Test
    void generateJwtToken_ShouldGenerateValidToken() {
        // Given
        Authentication authentication = mock(Authentication.class);
        User userPrincipal = new User();
        userPrincipal.setUsername("testuser");
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        // When
        String token = jwtUtils.generateJwtToken(authentication);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("testuser");
    }

    @Test
    void validateJwtToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.string";

        // When
        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateJwtToken_ShouldReturnFalse_WhenTokenIsExpired() throws InterruptedException {
        // Given
        JwtUtils shortLivedJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(shortLivedJwtUtils, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(shortLivedJwtUtils, "jwtExpirationMs", 1); // 1ms expiration

        Authentication authentication = mock(Authentication.class);
        User userPrincipal = new User();
        userPrincipal.setUsername("testuser");
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        String token = shortLivedJwtUtils.generateJwtToken(authentication);
        Thread.sleep(10); // Wait for expiration

        // When
        boolean isValid = shortLivedJwtUtils.validateJwtToken(token);

        // Then
        assertThat(isValid).isFalse();
    }
}
