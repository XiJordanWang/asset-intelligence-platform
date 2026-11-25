package io.github.xijordanwang.assetintel.dashboard.service;

import io.github.xijordanwang.assetintel.dashboard.dto.DashboardView;
import io.github.xijordanwang.assetintel.dashboard.dto.PlantView;
import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.dashboard.dto.DashboardView.*;
import io.github.xijordanwang.assetintel.site.entity.Site;
import io.github.xijordanwang.assetintel.site.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import io.github.xijordanwang.assetintel.asset.entity.AssetStatus;
import io.github.xijordanwang.assetintel.asset.entity.CapexProgramme;
import io.github.xijordanwang.assetintel.asset.entity.PlantArea;
import io.github.xijordanwang.assetintel.common.exception.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.xijordanwang.assetintel.asset.service.DepreciationCalculator.accumulatedDepreciation;
import static io.github.xijordanwang.assetintel.asset.service.DepreciationCalculator.netBookValue;

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

        List<Asset> all = assets.findAllWithSite();
        Map<AssetStatus, Long> byStatus = new EnumMap<>(AssetStatus.class);
        all.forEach(a -> byStatus.merge(a.getStatus(), 1L, Long::sum));

        return new DashboardView(asOf, totals, plants, points, monthly(all, asOf), byStatus);
    }

    /** One plant at the end of {@code asOf}: only assets in service by then and not yet disposed count. */
    public PlantView plant(String plantCode, LocalDate asOf) {
        List<Site> plantSites = sites.findAll().stream()
                .filter(s -> s.getPlantCode().equals(plantCode))
                .sorted(Comparator.comparing(Site::getCode))
                .toList();
        if (plantSites.isEmpty()) {
            throw new NotFoundException("No plant " + plantCode);
        }
        Site main = plantSites.get(0);
        List<Asset> onBooks = assets.findAllWithSite().stream()
                .filter(a -> a.getSite().getPlantCode().equals(plantCode))
                .filter(a -> a.onBooksAt(asOf))
                .toList();

        Map<PlantArea, List<Asset>> byArea = onBooks.stream().collect(Collectors.groupingBy(
                a -> a.getArea() != null ? a.getArea() : PlantArea.defaultFor(a.getCategory()),
                () -> new EnumMap<>(PlantArea.class), Collectors.toList()));
        List<PlantView.AreaSummary> areas = Arrays.stream(PlantArea.values())
                .map(area -> {
                    List<Asset> list = byArea.getOrDefault(area, List.of());
                    List<PlantView.TopAsset> top = list.stream()
                            .sorted(Comparator.comparing((Asset a) -> netBookValue(a, asOf)).reversed())
                            .limit(8)
                            .map(a -> new PlantView.TopAsset(a.getTag(), a.getName(), a.getCategory().name(),
                                    a.getSite().getCode(), netBookValue(a, asOf)))
                            .toList();
                    return new PlantView.AreaSummary(area.name(), area.label(), list.size(),
                            sum(list, Asset::getOriginalValue), sum(list, a -> netBookValue(a, asOf)), top);
                })
                .toList();

        Map<Optional<CapexProgramme>, List<Asset>> byProgramme = onBooks.stream()
                .collect(Collectors.groupingBy(a -> Optional.ofNullable(a.getProgramme())));
        List<PlantView.Slice> programmes = byProgramme.entrySet().stream()
                .map(e -> slice(e.getKey().map(Enum::name).orElse(null),
                        e.getKey().map(CapexProgramme::label).orElse("No programme"), e.getValue(), asOf))
                .sorted(Comparator.comparing(PlantView.Slice::netBookValue).reversed())
                .toList();

        List<PlantView.Slice> categories = onBooks.stream()
                .collect(Collectors.groupingBy(Asset::getCategory, () -> new EnumMap<>(AssetCategory.class),
                        Collectors.toList()))
                .entrySet().stream()
                .map(e -> slice(e.getKey().name(), e.getKey().name(), e.getValue(), asOf))
                .toList();

        return new PlantView(plantCode, main.getName(), main.getCity(), asOf, plantSites.size(), onBooks.size(),
                sum(onBooks, Asset::getOriginalValue), sum(onBooks, a -> accumulatedDepreciation(a, asOf)),
                sum(onBooks, a -> netBookValue(a, asOf)), areas, programmes, categories);
    }

    private static PlantView.Slice slice(String key, String label, List<Asset> list, LocalDate asOf) {
        return new PlantView.Slice(key, label, list.size(), sum(list, Asset::getOriginalValue),
                sum(list, a -> netBookValue(a, asOf)));
    }

    private List<MonthPoint> monthly(List<Asset> all, LocalDate asOf) {
        YearMonth last = YearMonth.from(asOf);
        List<MonthPoint> points = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth month = last.minusMonths(i);
            List<Asset> added = all.stream()
                    .filter(a -> a.getInServiceDate() != null && YearMonth.from(a.getInServiceDate()).equals(month))
                    .toList();
            List<Asset> retired = all.stream()
                    .filter(a -> a.getDisposedDate() != null && YearMonth.from(a.getDisposedDate()).equals(month))
                    .toList();
            points.add(new MonthPoint(month.toString(), added.size(), sum(added, Asset::getOriginalValue),
                    retired.size(), sum(retired, Asset::getOriginalValue)));
        }
        return points;
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
