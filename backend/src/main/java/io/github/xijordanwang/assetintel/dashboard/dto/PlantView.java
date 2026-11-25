package io.github.xijordanwang.assetintel.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One plant as it stood at the end of a month: totals, a zone per production area, and spend by programme. */
public record PlantView(
        String plantCode,
        String plantName,
        String city,
        LocalDate asOf,
        long siteCount,
        long assetCount,
        BigDecimal originalValue,
        BigDecimal accumulatedDepreciation,
        BigDecimal netBookValue,
        List<AreaSummary> areas,
        List<Slice> programmes,
        List<Slice> categories) {

    public record AreaSummary(String area, String label, long assetCount, BigDecimal originalValue,
                              BigDecimal netBookValue, List<TopAsset> topAssets) {
    }

    /** One slice of a breakdown; {@code key} is null for assets with no programme. */
    public record Slice(String key, String label, long assetCount, BigDecimal originalValue, BigDecimal netBookValue) {
    }

    public record TopAsset(String tag, String name, String category, String siteCode, BigDecimal netBookValue) {
    }
}
