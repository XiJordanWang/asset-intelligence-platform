package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.dashboard.service.DashboardService;
import io.github.xijordanwang.assetintel.dashboard.dto.PlantView;
import io.github.xijordanwang.assetintel.demo.service.DemoDataSeeder;
import io.github.xijordanwang.assetintel.demo.config.DemoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "viewer", roles = "ADMIN") // reads need a signed-in user who can see every area
@AutoConfigureMockMvc
@DirtiesContext
class PlantViewTest {

    @Autowired DemoDataSeeder seeder;
    @Autowired DemoProperties props;
    @Autowired DashboardService dashboard;
    @Autowired MockMvc mvc;

    @BeforeEach
    void seed() {
        seeder.seedIfEmpty(props);
    }

    @Test
    void areasAndProgrammesAddUpToThePlantTotal() {
        PlantView view = dashboard.plant("AKL", LocalDate.now());

        assertThat(view.assetCount()).isPositive();
        assertThat(view.areas()).hasSize(8);
        assertThat(view.areas().stream().mapToLong(PlantView.AreaSummary::assetCount).sum())
                .isEqualTo(view.assetCount());
        assertThat(view.areas().stream().map(PlantView.AreaSummary::netBookValue).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(view.netBookValue());
        assertThat(view.programmes().stream().map(PlantView.Slice::netBookValue).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(view.netBookValue());
        assertThat(view.areas()).allSatisfy(a -> assertThat(a.topAssets().size()).isLessThanOrEqualTo(8));
    }

    @Test
    void anEarlierMonthShowsFewerAssets() {
        PlantView now = dashboard.plant("HLZ", LocalDate.now());
        PlantView fiveYearsAgo = dashboard.plant("HLZ", LocalDate.now().minusYears(5));
        assertThat(fiveYearsAgo.assetCount()).isLessThan(now.assetCount());
    }

    @Test
    void servesThePlantOverHttpAndRejectsUnknownPlants() throws Exception {
        mvc.perform(get("/api/dashboard/plants/chc").param("upTo", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantCode").value("CHC"))
                .andExpect(jsonPath("$.asOf").value("2025-06-30"))
                .andExpect(jsonPath("$.areas.length()").value(8));
        mvc.perform(get("/api/dashboard/plants/XYZ")).andExpect(status().isNotFound());
    }
}
