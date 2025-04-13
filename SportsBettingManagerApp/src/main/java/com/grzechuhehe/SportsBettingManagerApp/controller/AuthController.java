package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.util.JwtUtils;
import com.grzechuhehe.SportsBettingManagerApp.dto.JwtResponse;
import com.grzechuhehe.SportsBettingManagerApp.dto.LoginRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.PasswordResetSubmit;
import com.grzechuhehe.SportsBettingManagerApp.dto.SignupRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    // Metoda GET dla testowania dostępności endpointu
//    @GetMapping("/signup")
//    public ResponseEntity<?> signupPage() {
//        return ResponseEntity.ok("Endpoint rejestracji dostępny. Użyj metody POST z odpowiednimi danymi.");
//    }

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // Wyświetl wiadomość debugowania
            System.out.println("Próba uwierzytelnienia użytkownika: " + loginRequest.getUsername());
            System.out.println("Hasło: " + loginRequest.getPassword().length() + " znaków");
            
            // Sprawdź czy użytkownik istnieje przed uwierzytelnieniem
            boolean userExists = userRepository.findByUsername(loginRequest.getUsername()).isPresent();
            System.out.println("Użytkownik istnieje? " + userExists);
            
            if (!userExists) {
                System.out.println("Użytkownik nie istnieje: " + loginRequest.getUsername());
                return ResponseEntity.status(401).body(
                    java.util.Map.of("error", "Użytkownik nie istnieje")
                );
            }
            
            try {
                // Przed uwierzytelnieniem sprawdź czy hasło jest poprawne, aby lepiej zdiagnozować problem
                User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();
                String rawPassword = loginRequest.getPassword();
                String encodedPassword = user.getPassword();
                
                System.out.println("Porównuję hasła:");
                System.out.println("- Hasło podane: " + rawPassword.length() + " znaków");
                System.out.println("- Hasło w bazie: " + encodedPassword);
                
                // Ręczne porównanie haseł
                boolean passwordMatches = encoder.matches(rawPassword, encodedPassword);
                System.out.println("Wynik porównania haseł: " + passwordMatches);
                
                if (!passwordMatches) {
                    System.out.println("Hasło nie pasuje - rzucam wyjątek");
                    throw new org.springframework.security.authentication.BadCredentialsException("Niepoprawne hasło");
                }
                
                // Standardowe uwierzytelnianie
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
                
                System.out.println("Uwierzytelnienie powiodło się");
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtils.generateJwtToken(authentication);
                System.out.println("Wygenerowany token JWT: " + jwt);
                
                return ResponseEntity.ok(new JwtResponse(jwt));
            } catch (Exception authEx) {
                System.out.println("Błąd uwierzytelniania: " + authEx.getClass().getName());
                System.out.println("Komunikat: " + authEx.getMessage());
                
                // Zapasowe logowanie (w razie problemów ze Spring Security)
                try {
                    // Jeśli zawiedzie standardowe uwierzytelnianie, spróbuj ręcznie zalogować
                    if (authEx instanceof org.springframework.security.authentication.BadCredentialsException) {
                        System.out.println("Próba zapasowego logowania");
                        return ResponseEntity.status(401).body(
                            java.util.Map.of("error", "Nieprawidłowe hasło")
                        );
                    }
                    
                    throw authEx;
                } catch (Exception fallbackEx) {
                    System.out.println("Błąd zapasowego logowania: " + fallbackEx.getMessage());
                    throw fallbackEx;
                }
            }
        } catch (Exception e) {
            // Dodaj log, aby zobaczyć dokładny błąd
            System.out.println("Wyjątek podczas logowania: " + e.getClass().getName());
            System.out.println("Komunikat: " + e.getMessage());
            e.printStackTrace();
            // Zwracamy odpowiedź JSON zamiast zwykłego tekstu
            return ResponseEntity.status(401).body(
                java.util.Map.of("error", "Nieprawidłowa nazwa użytkownika lub hasło")
            );
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest,
                                          BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
        }

        if (userRepository.findByUsername(signUpRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(
                java.util.Map.of("error", "Nazwa użytkownika jest już zajęta!")
            );
        }

        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(
                java.util.Map.of("error", "Email jest już w użyciu!")
            );
        }

        // Loguj informacje o rejestrowaniu użytkownika
        System.out.println("Rejestracja nowego użytkownika: " + signUpRequest.getUsername());
        System.out.println("Email: " + signUpRequest.getEmail());
        
        // Kodowanie hasła
        String rawPassword = signUpRequest.getPassword();
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("Hasło przed kodowaniem: " + rawPassword.length() + " znaków");
        System.out.println("Hasło po kodowaniu: " + encodedPassword);
        
        // Tworzenie nowego użytkownika
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encodedPassword);
        user.setRoles(Collections.singletonList("USER"));

        userRepository.save(user);
        return ResponseEntity.ok(
            java.util.Map.of("message", "Użytkownik zarejestrowany pomyślnie!")
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> handlePasswordReset(
            @Valid @RequestBody PasswordResetSubmit request,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(getValidationErrors(result));
        }

        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok(
                java.util.Map.of("message", "Hasło zostało zmienione pomyślnie")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(
                java.util.Map.of("error", e.getMessage())
            );
        }
    }

    private List<String> getValidationErrors(BindingResult result) {
        return result.getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());
    }
}