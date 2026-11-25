package io.github.xijordanwang.assetintel.asset.repository;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.entity.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    @EntityGraph(attributePaths = "site")
    Optional<Asset> findByTag(String tag);

    @EntityGraph(attributePaths = "site")
    @Query("""
            select a from Asset a
            where (:siteCode is null or a.site.code = :siteCode)
              and (:category is null or a.category = :category)
              and (:status is null or a.status = :status)
            """)
    Page<Asset> search(@Param("siteCode") String siteCode,
                       @Param("category") AssetCategory category,
                       @Param("status") AssetStatus status,
                       Pageable pageable);

    @EntityGraph(attributePaths = "site")
    @Query("select a from Asset a where a.status <> io.github.xijordanwang.assetintel.asset.entity.AssetStatus.DISPOSED")
    List<Asset> findAllActive();

    @EntityGraph(attributePaths = "site")
    @Query("select a from Asset a")
    List<Asset> findAllWithSite();
}
