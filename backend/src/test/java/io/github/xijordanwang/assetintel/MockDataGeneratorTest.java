package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.demo.service.MockDataGenerator;
import io.github.xijordanwang.assetintel.site.entity.Site;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MockDataGeneratorTest {

    @Test
    void sameSeedGivesSameCompany() {
        LocalDate today = LocalDate.of(2026, 11, 24);
        List<Site> a = new MockDataGenerator(42).sites();
        List<Site> b = new MockDataGenerator(42).sites();
        assertThat(a).extracting(Site::getCode).containsExactlyElementsOf(b.stream().map(Site::getCode).toList());
        assertThat(a).extracting(Site::getAnnualCapexBudget)
                .containsExactlyElementsOf(b.stream().map(Site::getAnnualCapexBudget).toList());
        assertThat(new MockDataGenerator(42).assets(a, 50, today)).extracting(Asset::getOriginalValue)
                .containsExactlyElementsOf(new MockDataGenerator(42).assets(b, 50, today).stream()
                        .map(Asset::getOriginalValue).toList());
    }

    @Test
    void companyHasFourPlantsAndAboutFiftyUniqueSitesInNewZealand() {
        List<Site> sites = new MockDataGenerator(1).sites();
        assertThat(sites).hasSize(48);
        assertThat(sites.stream().map(Site::getCode).collect(Collectors.toSet())).hasSize(48);
        assertThat(sites.stream().map(Site::getPlantCode).collect(Collectors.toSet()))
                .isEqualTo(Set.of("AKL", "HLZ", "CHC", "WLG"));
        assertThat(sites).allSatisfy(s -> {
            assertThat(s.getLatitude()).isBetween(-47.5, -34.0);
            assertThat(s.getLongitude()).isBetween(166.0, 179.0);
        });
    }
}
