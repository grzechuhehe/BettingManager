package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.PasswordResetSubmit;
import com.grzechuhehe.SportsBettingManagerApp.model.PasswordResetToken;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.PasswordResetTokenRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.email.EmailService;
import com.grzechuhehe.SportsBettingManagerApp.util.PiiUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void processForgotPassword(String email) {
        log.info("Żądanie resetu hasła dla: {}", PiiUtils.maskEmail(email));
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        // Zabezpieczenie przed User Enumeration: zawsze zachowujemy się tak samo na zewnątrz
        if (userOpt.isEmpty()) {
            log.warn("Żądanie resetu dla nieistniejącego konta: {}", PiiUtils.maskEmail(email));
            return; 
        }
        
        User user = userOpt.get();
        
        // Jeśli użytkownik ma już ważny token, możemy go usunąć i wygenerować nowy
        PasswordResetToken existingToken = tokenRepository.findByUser(user);
        if (existingToken != null) {
            tokenRepository.delete(existingToken);
        }

        String token = UUID.randomUUID().toString();
        createPasswordResetToken(user, token);
        
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    private void createPasswordResetToken(User user, String token) {
        PasswordResetToken myToken = new PasswordResetToken();
        myToken.setUser(user);
        myToken.setToken(token);
        tokenRepository.save(myToken);
        log.info("Zapisano nowy token resetowania hasła dla użytkownika {}", PiiUtils.maskEmail(user.getEmail()));
    }

    public void resetPassword(PasswordResetSubmit request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null) {
            throw new IllegalArgumentException("Nieprawidłowy token");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token wygasł");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);
        log.info("Hasło dla użytkownika {} zostało zresetowane pomyślnie", PiiUtils.maskEmail(user.getEmail()));
    }
}
