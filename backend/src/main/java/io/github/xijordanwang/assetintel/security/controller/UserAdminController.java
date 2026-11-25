package io.github.xijordanwang.assetintel.security.controller;

import io.github.xijordanwang.assetintel.security.config.SecurityConfig;
import io.github.xijordanwang.assetintel.security.entity.AppUser;
import io.github.xijordanwang.assetintel.security.entity.UserRole;
import io.github.xijordanwang.assetintel.security.repository.AppUserRepository;
import io.github.xijordanwang.assetintel.security.service.Actor;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.common.exception.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** User and role management, for admins only (see {@link SecurityConfig}). */
@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    public record RoleInfo(UserRole role, String description) {
    }

    public record UpdateRoles(Set<UserRole> roles) {
    }

    private final AppUserRepository users;

    public UserAdminController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AuthController.UserView> list() {
        return users.findAllByOrderByIdAsc().stream().map(AuthController.UserView::of).toList();
    }

    @GetMapping("/roles")
    public List<RoleInfo> roles() {
        return Arrays.stream(UserRole.values()).map(r -> new RoleInfo(r, r.description())).toList();
    }

    @PutMapping("/{username}/roles")
    @Transactional
    public AuthController.UserView updateRoles(@PathVariable String username, @RequestBody UpdateRoles body) {
        AppUser user = users.findByUsername(username).orElseThrow(() -> new NotFoundException("No user " + username));
        Set<UserRole> roles = body.roles() == null ? Set.of() : body.roles();
        if (username.equals(Actor.current().username()) && !roles.contains(UserRole.ADMIN)) {
            throw new BusinessRuleException("You cannot remove your own admin role");
        }
        user.replaceRoles(roles);
        return AuthController.UserView.of(user);
    }
}
