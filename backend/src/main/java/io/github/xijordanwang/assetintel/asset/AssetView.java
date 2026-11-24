package io.github.xijordanwang.assetintel.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

/** API representation of an asset, with values calculated as of a given date. */
public record AssetView(
        String tag,
        String name,
        AssetCategory category,
        AssetStatus status,
        String siteCode,
        String plantCode,
        String supplier,
        BigDecimal originalValue,
        BigDecimal accumulatedDepreciation,
        BigDecimal impairment,
        BigDecimal netBookValue,
        LocalDate inServiceDate,
        LocalDate lastVerifiedDate) {

    public static AssetView of(Asset a, LocalDate asOf) {
        return new AssetView(
                a.getTag(), a.getName(), a.getCategory(), a.getStatus(),
                a.getSite().getCode(), a.getSite().getPlantCode(), a.getSupplier(),
                a.getOriginalValue(),
                DepreciationCalculator.accumulatedDepreciation(a, asOf),
                a.getImpairment(),
                DepreciationCalculator.netBookValue(a, asOf),
                a.getInServiceDate(), a.getLastVerifiedDate());
    }
}
