package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.approval.InvestmentRequestRepository;
import io.github.xijordanwang.assetintel.demo.DemoDataSeeder;
import io.github.xijordanwang.assetintel.demo.DemoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class DemoSeedAndDashboardTest {

    @Autowired DemoDataSeeder seeder;
    @Autowired DemoProperties props;
    @Autowired InvestmentRequestRepository requests;
    @Autowired MockMvc mvc;

    @Test
    void seedsOnceAndServesTheCockpit() throws Exception {
        assertThat(seeder.seedIfEmpty(props)).isTrue();
        assertThat(seeder.seedIfEmpty(props)).isFalse();
        assertThat(requests.count()).isEqualTo(props.requestCount());

        mvc.perform(get("/api/dashboard/overview").param("asOf", "2026-11-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.siteCount").value(48))
                .andExpect(jsonPath("$.plants.length()").value(4))
                .andExpect(jsonPath("$.sites.length()").value(48))
                .andExpect(jsonPath("$.totals.netBookValue", greaterThan(0.0)));

        mvc.perform(get("/api/assets").param("category", "VEHICLE").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].category").value("VEHICLE"));

        mvc.perform(get("/api/sites")).andExpect(jsonPath("$.length()").value(48));
    }
}
