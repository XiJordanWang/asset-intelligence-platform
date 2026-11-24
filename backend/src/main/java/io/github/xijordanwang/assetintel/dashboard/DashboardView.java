package io.github.xijordanwang.assetintel.dashboard;

import io.github.xijordanwang.assetintel.asset.AssetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Numbers for the management cockpit: company totals, a breakdown per plant, and sites for the map. */
public record DashboardView(
        LocalDate asOf,
        Totals totals,
        List<PlantSummary> plants,
        List<SitePoint> sites) {

    public record Totals(long assetCount, long siteCount, long supplierCount,
                         BigDecimal originalValue, BigDecimal accumulatedDepreciation,
                         BigDecimal impairment, BigDecimal netBookValue) {
    }

    public record CategorySummary(BigDecimal netBookValue, BigDecimal depreciatedPercent) {
    }

    public record PlantSummary(String plantCode, BigDecimal originalValue, BigDecimal netBookValue,
                               BigDecimal yearToDateDepreciation,
                               Map<AssetCategory, CategorySummary> byCategory) {
    }

    public record SitePoint(String code, String name, String plantCode, String city,
                            double latitude, double longitude, long assetCount, BigDecimal netBookValue) {
    }
}
