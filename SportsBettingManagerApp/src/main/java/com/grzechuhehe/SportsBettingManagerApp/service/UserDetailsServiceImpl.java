package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("loadUserByUsername wywołane dla: " + username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                System.out.println("Użytkownik nie znaleziony: " + username);
                return new UsernameNotFoundException("Użytkownik nie znaleziony: " + username);
            });
        
        System.out.println("Znaleziono użytkownika: " + user.getUsername() + ", hasło: " + (user.getPassword() != null ? "ustawione" : "brak"));
        System.out.println("Role: " + user.getRoles());
        
        return user;
    }
}