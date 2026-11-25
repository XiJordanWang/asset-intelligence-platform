package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.asset.entity.AssetStatus;
import io.github.xijordanwang.assetintel.site.entity.Site;
import io.github.xijordanwang.assetintel.site.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "viewer", roles = "ADMIN") // reads need a signed-in user who can see every area
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StocktakeWorkflowTest {

    @Autowired MockMvc mvc;

    /** The user the next POST is sent as. Read when the request runs, so a helper can switch it just before. */
    private org.springframework.test.web.servlet.request.RequestPostProcessor actor = TestUsers.as("aroha");

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String url, Object... vars) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url, vars)
                .with(request -> actor.postProcessRequest(request));
    }
    @Autowired SiteRepository sites;
    @Autowired AssetRepository assets;

    @BeforeEach
    void data() {
        Site a = new Site("HLZ-01", "Hamilton plant", "HLZ", "Hamilton", "Waikato", -37.78, 175.27, new BigDecimal("1000000"));
        a.assignCoordinator("kiri");
        Site b = new Site("HLZ-02", "Cambridge depot", "HLZ", "Cambridge", "Waikato", -37.89, 175.47, new BigDecimal("500000"));
        b.assignCoordinator("tom");
        Site c = new Site("AKL-01", "Auckland plant", "AKL", "Auckland", "Auckland", -36.85, 174.76, new BigDecimal("1000000"));
        sites.save(a);
        sites.save(b);
        sites.save(c);
        LocalDate d = LocalDate.now().minusYears(1);
        assets.save(new Asset("AST-1", "Laptop", AssetCategory.IT_EQUIPMENT, AssetStatus.IN_SERVICE, a, null, new BigDecimal("3000"), d));
        assets.save(new Asset("AST-2", "Forklift", AssetCategory.VEHICLE, AssetStatus.IN_SERVICE, a, null, new BigDecimal("40000"), d));
        assets.save(new Asset("AST-3", "Scanner", AssetCategory.IT_EQUIPMENT, AssetStatus.IN_SERVICE, b, null, new BigDecimal("900"), d));
        assets.save(new Asset("AST-4", "Server", AssetCategory.IT_EQUIPMENT, AssetStatus.IN_SERVICE, c, null, new BigDecimal("20000"), d));
    }

    private String plan(String plant, int sample) throws Exception {
        actor = TestUsers.as("grace");
        String body = mvc.perform(post("/api/stocktakes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Count","scopePlant":"%s","samplePercent":%d,"dueDate":"2030-01-01","createdBy":"grace"}"""
                        .formatted(plant, sample)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.reference");
    }

    private ResultActions count(String ref, String site, String tag, String result, String by) throws Exception {
        actor = TestUsers.as(by);
        return mvc.perform(post("/api/stocktakes/{r}/sites/{s}/count", ref, site).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetTag\":\"%s\",\"result\":\"%s\",\"countedBy\":\"%s\"}".formatted(tag, result, by)));
    }

    private ResultActions submit(String ref, String site, String by) throws Exception {
        actor = TestUsers.as(by);
        return mvc.perform(post("/api/stocktakes/{r}/sites/{s}/submit", ref, site).contentType(MediaType.APPLICATION_JSON)
                .content("{\"submittedBy\":\"%s\"}".formatted(by)));
    }

    @Test
    void fullCountFeedsResultsBackIntoTheRegister() throws Exception {
        String ref = plan("HLZ", 100);
        mvc.perform(post("/api/stocktakes/{r}/start", ref))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sites", hasSize(2)))
                .andExpect(jsonPath("$.total").value(3));

        // Only the site's coordinator may count.
        count(ref, "HLZ-01", "AST-1", "FOUND", "tom").andExpect(status().isConflict());
        count(ref, "HLZ-01", "AST-1", "FOUND", "kiri").andExpect(jsonPath("$.counted").value(1));
        // Cannot submit with uncounted assets.
        submit(ref, "HLZ-01", "kiri").andExpect(status().isConflict());
        count(ref, "HLZ-01", "AST-2", "DAMAGED", "kiri");
        submit(ref, "HLZ-01", "kiri").andExpect(jsonPath("$.status").value("SUBMITTED"));

        // Cannot complete while a site is still open.
        actor = TestUsers.as("grace");
        mvc.perform(post("/api/stocktakes/{r}/complete", ref)).andExpect(status().isConflict());
        count(ref, "HLZ-02", "AST-3", "MISSING", "tom");
        submit(ref, "HLZ-02", "tom");

        actor = TestUsers.as("grace");
        mvc.perform(post("/api/stocktakes/{r}/complete", ref))
                .andExpect(jsonPath("$.campaign.status").value("COMPLETED"))
                .andExpect(jsonPath("$.verified").value(2))
                .andExpect(jsonPath("$.draftDisposals", hasSize(1)));

        mvc.perform(get("/api/assets/AST-1")).andExpect(jsonPath("$.lastVerifiedDate").value(LocalDate.now(java.time.ZoneId.of("Pacific/Auckland")).toString()));
        mvc.perform(get("/api/assets/AST-2")).andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));
        mvc.perform(get("/api/changes")).andExpect(jsonPath("$[0].disposalReason").value("LOST"))
                .andExpect(jsonPath("$[0].assetTag").value("AST-3"));
        // Auckland was out of scope.
        mvc.perform(get("/api/assets/AST-4")).andExpect(jsonPath("$.lastVerifiedDate").doesNotExist());
    }

    @Test
    void cannotStartWhileASiteHasNoCoordinator() throws Exception {
        String ref = plan("AKL", 100);
        mvc.perform(post("/api/stocktakes/{r}/start", ref))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("AKL-01")));
    }

    @Test
    void cannotStartWhileAnInScopeAssetHasAnOpenForm() throws Exception {
        actor = TestUsers.as("a");
        String body = mvc.perform(post("/api/changes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRANSFER\",\"assetTag\":\"AST-1\",\"requestedBy\":\"a\",\"targetSiteCode\":\"HLZ-02\"}"))
                .andReturn().getResponse().getContentAsString();
        String change = com.jayway.jsonpath.JsonPath.read(body, "$.reference");
        mvc.perform(post("/api/changes/{r}/submit", change));

        String ref = plan("HLZ", 100);
        mvc.perform(post("/api/stocktakes/{r}/start", ref))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(change)));
    }

    @Test
    void samplingCountsAtLeastOneAssetPerSite() throws Exception {
        String ref = plan("HLZ", 10);
        mvc.perform(post("/api/stocktakes/{r}/start", ref))
                .andExpect(jsonPath("$.sites[0].total").value(1))
                .andExpect(jsonPath("$.sites[1].total").value(1));
        mvc.perform(get("/api/stocktakes/{r}/sites/{s}", ref, "HLZ-01")).andExpect(jsonPath("$.lines", hasSize(1)));
    }
}
