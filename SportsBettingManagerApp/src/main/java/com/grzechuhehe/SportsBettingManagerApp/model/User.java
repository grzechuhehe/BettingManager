package com.grzechuhehe.SportsBettingManagerApp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank(message = "Invalid email format")
    @Size(max = 60)
    private String email;

    @NotBlank(message = "Password must be at least 6 characters long")
    @Size(max = 120)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        System.out.println("getAuthorities wywołane dla: " + this.username);
        System.out.println("Role: " + this.roles);
        
        return roles.stream()
                .map(role -> {
                    // Upewnij się, że rola ma prefiks ROLE_
                    String roleWithPrefix = !role.startsWith("ROLE_") ? "ROLE_" + role : role;
                    System.out.println("Dodaję rolę: " + roleWithPrefix);
                    return new SimpleGrantedAuthority(roleWithPrefix);
                })
                .collect(Collectors.toList());
    }


    @Override
    public boolean isAccountNonExpired() { 
        System.out.println("isAccountNonExpired: true"); 
        return true; 
    }
    
    @Override
    public boolean isAccountNonLocked() { 
        System.out.println("isAccountNonLocked: true"); 
        return true; 
    }
    
    @Override
    public boolean isCredentialsNonExpired() { 
        System.out.println("isCredentialsNonExpired: true"); 
        return true; 
    }
    
    @Override
    public boolean isEnabled() { 
        System.out.println("isEnabled: true"); 
        return true; 
    }
}