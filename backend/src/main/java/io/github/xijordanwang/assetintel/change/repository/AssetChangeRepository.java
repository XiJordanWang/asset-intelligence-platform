package io.github.xijordanwang.assetintel.change.repository;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.ChangeStatus;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetChangeRepository extends JpaRepository<AssetChange, Long> {

    @EntityGraph(attributePaths = {"asset", "asset.site", "targetSite", "steps"})
    Optional<AssetChange> findByReference(String reference);

    @EntityGraph(attributePaths = {"asset", "asset.site", "targetSite"})
    List<AssetChange> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"asset", "asset.site", "targetSite"})
    List<AssetChange> findByStatusOrderByCreatedAtDesc(ChangeStatus status);

    long countByType(ChangeType type);
}
