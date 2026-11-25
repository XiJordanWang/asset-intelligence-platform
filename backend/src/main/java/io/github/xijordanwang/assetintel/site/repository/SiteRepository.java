package io.github.xijordanwang.assetintel.site.repository;

import io.github.xijordanwang.assetintel.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByCode(String code);
}
