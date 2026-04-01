package server.main.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String loginId;
    private final String userType;  // MEMBER | ADMIN
    private final String role;      // ROLE_USER | ROLE_ADMIN

    public CustomUserPrincipal(Long id, String loginId, String userType, String role) {
        this.id = id;
        this.loginId = loginId;
        this.userType = userType;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return loginId;
    }
}
