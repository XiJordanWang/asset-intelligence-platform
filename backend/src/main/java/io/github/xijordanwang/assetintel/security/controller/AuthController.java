package io.github.xijordanwang.assetintel.security.controller;

import io.github.xijordanwang.assetintel.security.entity.AppUser;
import io.github.xijordanwang.assetintel.security.entity.UserRole;
import io.github.xijordanwang.assetintel.security.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** Sign-in for the React app. Demo users all share the password shown on the sign-in page. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserView(String username, String displayName, String title, String plantCode, Set<UserRole> roles) {
        static UserView of(AppUser u) {
            return new UserView(u.getUsername(), u.getDisplayName(), u.getTitle(), u.getPlantCode(), u.getRoles());
        }
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final AppUserRepository users;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
                          AppUserRepository users) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.users = users;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                                   HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                            "Wrong username or password"));
        }
        request.getSession(true);
        request.changeSessionId(); // a new session id after sign-in prevents session fixation
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(UserView.of(users.findByUsername(auth.getName()).orElseThrow()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /** The signed-in user, or 204 No Content for a visitor who has not signed in. */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserView> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return ResponseEntity.noContent().build(); // browsing without signing in is normal, not an error
        }
        return users.findByUsername(auth.getName()).map(UserView::of).map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** The demo people a visitor can sign in as. No passwords are returned. */
    @GetMapping("/personas")
    @Transactional(readOnly = true)
    public List<UserView> personas() {
        return users.findAllByOrderByIdAsc().stream().filter(AppUser::isEnabled).map(UserView::of).toList();
    }
}
