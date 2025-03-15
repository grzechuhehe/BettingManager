package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.PasswordResetSubmit;
import com.grzechuhehe.SportsBettingManagerApp.model.PasswordResetToken;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.PasswordResetTokenRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String createPasswordResetTokenForUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Nie znaleziono użytkownika z podanym adresem e-mail");
        }

        String token = UUID.randomUUID().toString();
        createPasswordResetToken(user, token);
        return token;
    }

    private void createPasswordResetToken(User user, String token) {
        PasswordResetToken myToken = new PasswordResetToken();
        myToken.setUser(user);
        myToken.setToken(token);
        tokenRepository.save(myToken);

        // Logowanie tokenu zamiast wysyłania e-mailem
        log.info("Token resetowania hasła dla użytkownika {}: {}", user.getEmail(), token);
    }

    public void validateResetToken(String token) {
        PasswordResetToken passToken = tokenRepository.findByToken(token);
        if (passToken == null || passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Nieprawidłowy lub przedawniony token");
        }
    }

    public void resetPassword(PasswordResetSubmit request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null) {
            throw new RuntimeException("Nieprawidłowy token");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token wygasł");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);
    }
}