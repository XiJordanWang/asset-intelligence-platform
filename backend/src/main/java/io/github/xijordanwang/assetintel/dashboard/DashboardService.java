package io.github.xijordanwang.assetintel.dashboard;

import io.github.xijordanwang.assetintel.asset.Asset;
import io.github.xijordanwang.assetintel.asset.AssetCategory;
import io.github.xijordanwang.assetintel.asset.AssetRepository;
import io.github.xijordanwang.assetintel.dashboard.DashboardView.*;
import io.github.xijordanwang.assetintel.site.Site;
import io.github.xijordanwang.assetintel.site.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.xijordanwang.assetintel.asset.DepreciationCalculator.accumulatedDepreciation;
import static io.github.xijordanwang.assetintel.asset.DepreciationCalculator.netBookValue;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AssetRepository assets;
    private final SiteRepository sites;

    public DashboardService(AssetRepository assets, SiteRepository sites) {
        this.assets = assets;
        this.sites = sites;
    }

    public DashboardView overview(LocalDate asOf) {
        List<Asset> active = assets.findAllActive();
        LocalDate startOfYear = asOf.withDayOfYear(1);

        Totals totals = new Totals(
                active.size(),
                sites.count(),
                active.stream().map(Asset::getSupplier).filter(Objects::nonNull).distinct().count(),
                sum(active, Asset::getOriginalValue),
                sum(active, a -> accumulatedDepreciation(a, asOf)),
                sum(active, Asset::getImpairment),
                sum(active, a -> netBookValue(a, asOf)));

        Map<String, List<Asset>> byPlant = active.stream()
                .collect(Collectors.groupingBy(a -> a.getSite().getPlantCode(), TreeMap::new, Collectors.toList()));
        List<PlantSummary> plants = byPlant.entrySet().stream()
                .map(e -> plantSummary(e.getKey(), e.getValue(), asOf, startOfYear))
                .toList();

        Map<Long, List<Asset>> bySite = active.stream().collect(Collectors.groupingBy(a -> a.getSite().getId()));
        List<SitePoint> points = sites.findAll().stream()
                .sorted(Comparator.comparing(Site::getCode))
                .map(s -> {
                    List<Asset> list = bySite.getOrDefault(s.getId(), List.of());
                    return new SitePoint(s.getCode(), s.getName(), s.getPlantCode(), s.getCity(),
                            s.getLatitude(), s.getLongitude(), list.size(), sum(list, a -> netBookValue(a, asOf)));
                })
                .toList();

        return new DashboardView(asOf, totals, plants, points);
    }

    private PlantSummary plantSummary(String plantCode, List<Asset> list, LocalDate asOf, LocalDate startOfYear) {
        Map<AssetCategory, CategorySummary> byCategory = new EnumMap<>(AssetCategory.class);
        list.stream().collect(Collectors.groupingBy(Asset::getCategory))
                .forEach((category, items) -> {
                    BigDecimal original = sum(items, Asset::getOriginalValue);
                    BigDecimal depreciated = sum(items, a -> accumulatedDepreciation(a, asOf));
                    BigDecimal pct = original.signum() == 0 ? BigDecimal.ZERO
                            : depreciated.multiply(BigDecimal.valueOf(100)).divide(original, 2, RoundingMode.HALF_UP);
                    byCategory.put(category, new CategorySummary(sum(items, a -> netBookValue(a, asOf)), pct));
                });
        BigDecimal ytd = sum(list, a -> accumulatedDepreciation(a, asOf)
                .subtract(accumulatedDepreciation(a, startOfYear)));
        return new PlantSummary(plantCode, sum(list, Asset::getOriginalValue),
                sum(list, a -> netBookValue(a, asOf)), ytd, byCategory);
    }

    private static BigDecimal sum(Collection<Asset> items, java.util.function.Function<Asset, BigDecimal> f) {
        return items.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
