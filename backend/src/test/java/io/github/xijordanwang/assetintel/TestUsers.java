package io.github.xijordanwang.assetintel;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/** Signs a MockMvc request in as one of the short test names used across the workflow tests. */
final class TestUsers {

    private record Who(String role, String plant) {
    }

    private static final Map<String, Who> USERS = Map.ofEntries(
            Map.entry("aroha", new Who("REQUESTER", "HLZ")),
            Map.entry("ben", new Who("REQUESTER", "HLZ")),
            Map.entry("mere", new Who("REQUESTER", "HLZ")),
            Map.entry("a", new Who("REQUESTER", "HLZ")),
            Map.entry("tama", new Who("PLANT_MANAGER", "HLZ")),
            Map.entry("nikau", new Who("PLANT_MANAGER", "AKL")),
            Map.entry("grace", new Who("FINANCE_CONTROLLER", null)),
            Map.entry("hemi", new Who("CFO", null)),
            Map.entry("kiri", new Who("SITE_COORDINATOR", "HLZ")),
            Map.entry("tom", new Who("SITE_COORDINATOR", "HLZ")),
            Map.entry("sam", new Who("ADMIN", null)));

    private TestUsers() {
    }

    static RequestPostProcessor as(String name) {
        Who who = USERS.get(name);
        if (who == null) {
            throw new IllegalArgumentException("No test user " + name);
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + who.role()));
        if (who.plant() != null) {
            authorities.add(new SimpleGrantedAuthority("PLANT_" + who.plant()));
        }
        RequestPostProcessor signIn = user(name).authorities(authorities);
        return request -> csrf().postProcessRequest(signIn.postProcessRequest(request));
    }
}
