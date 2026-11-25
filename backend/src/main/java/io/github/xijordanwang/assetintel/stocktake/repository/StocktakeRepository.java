package io.github.xijordanwang.assetintel.stocktake.repository;

import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StocktakeRepository extends JpaRepository<StocktakeCampaign, Long> {

    Optional<StocktakeCampaign> findByReference(String reference);

    List<StocktakeCampaign> findAllByOrderByCreatedAtDesc();
}
