package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.service.DepreciationCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DepreciationCalculatorTest {

    private static final LocalDate START = LocalDate.of(2024, 1, 15);

    @Test
    void itEquipmentDepreciatesOverFourYears() {
        // 12 of 48 months charged -> a quarter of the value.
        BigDecimal dep = DepreciationCalculator.accumulatedDepreciation(
                AssetCategory.IT_EQUIPMENT, new BigDecimal("4800.00"), START, LocalDate.of(2025, 1, 20));
        assertThat(dep).isEqualByComparingTo("1200.00");
    }

    @Test
    void depreciationStopsAtOriginalValue() {
        BigDecimal dep = DepreciationCalculator.accumulatedDepreciation(
                AssetCategory.IT_EQUIPMENT, new BigDecimal("4800.00"), START, LocalDate.of(2035, 1, 1));
        assertThat(dep).isEqualByComparingTo("4800.00");
    }

    @Test
    void landAndConstructionAreNotDepreciated() {
        assertThat(DepreciationCalculator.accumulatedDepreciation(
                AssetCategory.LAND, new BigDecimal("1000000"), START, LocalDate.of(2030, 1, 1))).isZero();
        assertThat(DepreciationCalculator.accumulatedDepreciation(
                AssetCategory.CONSTRUCTION_IN_PROGRESS, new BigDecimal("1000000"), null, LocalDate.of(2030, 1, 1))).isZero();
    }

    @Test
    void nothingIsChargedBeforeServiceStarts() {
        assertThat(DepreciationCalculator.accumulatedDepreciation(
                AssetCategory.VEHICLE, new BigDecimal("50000"), START, LocalDate.of(2023, 6, 1))).isZero();
    }
}
