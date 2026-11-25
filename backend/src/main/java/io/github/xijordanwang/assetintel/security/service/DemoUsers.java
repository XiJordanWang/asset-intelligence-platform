package io.github.xijordanwang.assetintel.security.service;

import io.github.xijordanwang.assetintel.security.entity.AppUser;
import io.github.xijordanwang.assetintel.security.entity.UserRole;
import io.github.xijordanwang.assetintel.security.repository.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static io.github.xijordanwang.assetintel.security.entity.UserRole.*;

/**
 * Creates the fictional Kowhai Components people on first start. They all use the password {@value #PASSWORD},
 * which the sign-in page shows, because this is a public demo with generated data.
 */
@Component
@Order(0)
public class DemoUsers implements ApplicationRunner {

    public static final String PASSWORD = "demo";

    record Person(String username, String name, String title, String plant, Set<UserRole> roles) {
    }

    public static final List<String> COORDINATORS = List.of("mia.coordinator", "pita.coordinator",
            "kiri.coordinator", "leo.coordinator");

    static final List<Person> PEOPLE = List.of(
            new Person("aroha.requester", "Aroha Ngata", "Maintenance engineer, Hamilton", "HLZ", Set.of(REQUESTER)),
            new Person("tama.plantmanager", "Tama Wiremu", "Plant manager, Hamilton", "HLZ", Set.of(PLANT_MANAGER)),
            new Person("nikau.plantmanager", "Nikau Rangi", "Plant manager, Auckland", "AKL", Set.of(PLANT_MANAGER)),
            new Person("ruby.plantmanager", "Ruby Chen", "Plant manager, Christchurch", "CHC", Set.of(PLANT_MANAGER)),
            new Person("eli.plantmanager", "Eli Thompson", "Plant manager, Wellington", "WLG", Set.of(PLANT_MANAGER)),
            new Person("grace.finance", "Grace Lee", "Finance controller", null, Set.of(FINANCE_CONTROLLER)),
            new Person("hemi.cfo", "Hemi Walker", "Chief financial officer", null, Set.of(CFO)),
            new Person("mia.coordinator", "Mia Patel", "Site coordinator, Auckland", "AKL", Set.of(SITE_COORDINATOR)),
            new Person("pita.coordinator", "Pita Harris", "Site coordinator, Hamilton", "HLZ", Set.of(SITE_COORDINATOR)),
            new Person("kiri.coordinator", "Kiri Jones", "Site coordinator, Christchurch", "CHC", Set.of(SITE_COORDINATOR)),
            new Person("leo.coordinator", "Leo Nguyen", "Site coordinator, Wellington", "WLG", Set.of(SITE_COORDINATOR)),
            new Person("sam.admin", "Sam King", "System administrator", null, Set.of(ADMIN)));

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DemoUsers(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String hash = encoder.encode(PASSWORD);
        PEOPLE.forEach(p -> users.save(new AppUser(p.username(), p.name(), p.title(), hash, p.plant(), p.roles())));
    }
}
