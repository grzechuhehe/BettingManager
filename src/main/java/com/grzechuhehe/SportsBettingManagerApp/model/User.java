package com.grzechuhehe.SportsBettingManagerApp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// ... imports

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20)
    private String username;

    @Size(max = 60)
    private String email;

    @Size(max = 120)
    private String password;

    private boolean isActiveUser = true;

    private Integer evEdgeThreshold = 2; // Default 2%

    private String xUsername;

    private String xProfileUrl;

    private LocalDateTime lastXCheckAt;

    private LocalDateTime registeredAt;

    private String lastScrapedTweetId;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

    private boolean isEmailVerified = false;
    
    private String verificationToken;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> {
                    String roleWithPrefix = !role.startsWith("ROLE_") ? "ROLE_" + role : role;
                    return new SimpleGrantedAuthority(roleWithPrefix);
                })
                .collect(Collectors.toList());
    }


    @Override
    public boolean isAccountNonExpired() { 
        return true; 
    }
    
    @Override
    public boolean isAccountNonLocked() { 
        return true; 
    }
    
    @Override
    public boolean isCredentialsNonExpired() { 
        return true; 
    }
    
    @Override
    public boolean isEnabled() { 
        return isActiveUser; 
    }
}