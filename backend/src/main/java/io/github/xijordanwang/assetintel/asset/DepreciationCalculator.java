package io.github.xijordanwang.assetintel.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Straight-line depreciation with no residual value, charged from the month after the asset
 * goes into service. Kept free of Spring so it is easy to unit test.
 */
public final class DepreciationCalculator {

    private DepreciationCalculator() {
    }

    public static BigDecimal accumulatedDepreciation(Asset asset, LocalDate asOf) {
        return accumulatedDepreciation(asset.getCategory(), asset.getOriginalValue(), asset.getInServiceDate(), asOf);
    }

    public static BigDecimal accumulatedDepreciation(AssetCategory category, BigDecimal originalValue,
                                                     LocalDate inServiceDate, LocalDate asOf) {
        if (!category.isDepreciable() || inServiceDate == null || !asOf.isAfter(inServiceDate)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long months = ChronoUnit.MONTHS.between(inServiceDate.withDayOfMonth(1), asOf.withDayOfMonth(1));
        long charged = Math.min(months, category.usefulLifeMonths());
        return originalValue
                .multiply(BigDecimal.valueOf(charged))
                .divide(BigDecimal.valueOf(category.usefulLifeMonths()), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal netBookValue(Asset asset, LocalDate asOf) {
        BigDecimal nbv = asset.getOriginalValue()
                .subtract(accumulatedDepreciation(asset, asOf))
                .subtract(asset.getImpairment());
        return nbv.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
