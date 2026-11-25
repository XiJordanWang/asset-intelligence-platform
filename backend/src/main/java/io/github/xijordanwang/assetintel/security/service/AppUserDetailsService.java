package io.github.xijordanwang.assetintel.security.service;

import io.github.xijordanwang.assetintel.security.entity.AppUser;
import io.github.xijordanwang.assetintel.security.repository.AppUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(authorities(user))
                .build();
    }

    public static List<GrantedAuthority> authorities(AppUser user) {
        List<GrantedAuthority> list = new ArrayList<>();
        user.getRoles().forEach(r -> list.add(new SimpleGrantedAuthority("ROLE_" + r.name())));
        if (user.getPlantCode() != null) {
            list.add(new SimpleGrantedAuthority(Actor.PLANT_PREFIX + user.getPlantCode()));
        }
        return list;
    }
}
