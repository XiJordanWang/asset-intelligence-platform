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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "viewer", roles = "ADMIN") // reads need a signed-in user who can see every area
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AssetChangeWorkflowTest {

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
        Site hlz = sites.save(new Site("HLZ-01", "Hamilton plant", "HLZ", "Hamilton", "Waikato", -37.78, 175.27, new BigDecimal("1000000")));
        sites.save(new Site("HLZ-02", "Cambridge depot", "HLZ", "Cambridge", "Waikato", -37.89, 175.47, new BigDecimal("500000")));
        sites.save(new Site("AKL-01", "Auckland plant", "AKL", "Auckland", "Auckland", -36.85, 174.76, new BigDecimal("1000000")));
        LocalDate recent = LocalDate.now().minusMonths(2);
        assets.save(new Asset("AST-1", "Laptop", AssetCategory.IT_EQUIPMENT, AssetStatus.IN_SERVICE, hlz, "Kauri IT", new BigDecimal("3000"), recent));
        assets.save(new Asset("AST-2", "Robotic welding cell", AssetCategory.PLANT_MACHINERY, AssetStatus.IN_SERVICE, hlz, "Kea Robotics", new BigDecimal("900000"), recent));
    }

    private String create(String json) throws Exception {
        actor = TestUsers.as(com.jayway.jsonpath.JsonPath.read(json, "$.requestedBy"));
        String body = mvc.perform(post("/api/changes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.reference");
    }

    private ResultActions decide(String ref, String action, String role, String by) throws Exception {
        actor = TestUsers.as(by);
        return mvc.perform(post("/api/changes/{r}/{a}", ref, action).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"%s\",\"decidedBy\":\"%s\"}".formatted(role, by)));
    }

    @Test
    void transferBetweenPlantsNeedsReceivingPlantAndFinanceThenMovesTheAsset() throws Exception {
        String ref = create("""
                {"type":"TRANSFER","assetTag":"AST-1","requestedBy":"aroha","targetSiteCode":"AKL-01"}""");
        mvc.perform(post("/api/changes/{r}/submit", ref))
                .andExpect(jsonPath("$.steps[*].role", contains("PLANT_MANAGER", "RECEIVING_PLANT_MANAGER", "FINANCE_CONTROLLER")));
        mvc.perform(get("/api/assets/AST-1")).andExpect(jsonPath("$.lockedByChange").value(ref));

        decide(ref, "approve", "PLANT_MANAGER", "tama");
        decide(ref, "approve", "RECEIVING_PLANT_MANAGER", "nikau");
        decide(ref, "approve", "FINANCE_CONTROLLER", "grace").andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/assets/AST-1"))
                .andExpect(jsonPath("$.siteCode").value("AKL-01"))
                .andExpect(jsonPath("$.lockedByChange").doesNotExist());
    }

    @Test
    void transferWithinAPlantNeedsOnlyThePlantManager() throws Exception {
        String ref = create("""
                {"type":"TRANSFER","assetTag":"AST-1","requestedBy":"aroha","targetSiteCode":"HLZ-02"}""");
        mvc.perform(post("/api/changes/{r}/submit", ref)).andExpect(jsonPath("$.steps.length()").value(1));
    }

    @Test
    void highValueDisposalEscalatesToCfoAndRetiresTheAsset() throws Exception {
        String ref = create("""
                {"type":"DISPOSAL","assetTag":"AST-2","requestedBy":"ben","disposalReason":"TECHNOLOGY_UPGRADE"}""");
        mvc.perform(post("/api/changes/{r}/submit", ref))
                .andExpect(jsonPath("$.steps[*].role", contains("PLANT_MANAGER", "FINANCE_CONTROLLER", "CFO")));
        decide(ref, "approve", "PLANT_MANAGER", "tama");
        decide(ref, "approve", "FINANCE_CONTROLLER", "grace");
        decide(ref, "approve", "CFO", "hemi");
        mvc.perform(get("/api/assets/AST-2"))
                .andExpect(jsonPath("$.status").value("DISPOSED"))
                .andExpect(jsonPath("$.disposedDate").exists());

        // A disposed asset cannot be changed again.
        actor = TestUsers.as("ben");
        mvc.perform(post("/api/changes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRANSFER\",\"assetTag\":\"AST-2\",\"requestedBy\":\"ben\",\"targetSiteCode\":\"AKL-01\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void lostAssetsAlwaysNeedFinanceEvenWhenCheap() throws Exception {
        String ref = create("""
                {"type":"DISPOSAL","assetTag":"AST-1","requestedBy":"mere","disposalReason":"LOST"}""");
        mvc.perform(post("/api/changes/{r}/submit", ref))
                .andExpect(jsonPath("$.steps[*].role", contains("PLANT_MANAGER", "FINANCE_CONTROLLER")));
    }

    @Test
    void anAssetCanOnlyHaveOneOpenChangeAndCancellingReleasesIt() throws Exception {
        String first = create("""
                {"type":"TRANSFER","assetTag":"AST-1","requestedBy":"aroha","targetSiteCode":"HLZ-02"}""");
        mvc.perform(post("/api/changes/{r}/submit", first));

        String second = create("""
                {"type":"DISPOSAL","assetTag":"AST-1","requestedBy":"mere","disposalReason":"DAMAGED"}""");
        mvc.perform(post("/api/changes/{r}/submit", second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(first)));

        // Only the requester may cancel.
        actor = TestUsers.as("mere");
        mvc.perform(post("/api/changes/{r}/cancel", first).contentType(MediaType.APPLICATION_JSON).content("{\"requestedBy\":\"mere\"}"))
                .andExpect(status().isConflict());
        actor = TestUsers.as("aroha");
        mvc.perform(post("/api/changes/{r}/cancel", first).contentType(MediaType.APPLICATION_JSON).content("{\"requestedBy\":\"aroha\"}"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(post("/api/changes/{r}/submit", second)).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    void rejectionUnlocksWithoutChangingTheAsset() throws Exception {
        String ref = create("""
                {"type":"TRANSFER","assetTag":"AST-1","requestedBy":"aroha","targetSiteCode":"AKL-01"}""");
        mvc.perform(post("/api/changes/{r}/submit", ref));
        decide(ref, "reject", "PLANT_MANAGER", "tama").andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(get("/api/assets/AST-1"))
                .andExpect(jsonPath("$.siteCode").value("HLZ-01"))
                .andExpect(jsonPath("$.lockedByChange").doesNotExist());
    }

    @Test
    void transferToTheSameSiteIsRefused() throws Exception {
        mvc.perform(post("/api/changes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRANSFER\",\"assetTag\":\"AST-1\",\"requestedBy\":\"a\",\"targetSiteCode\":\"HLZ-01\"}"))
                .andExpect(status().isConflict());
    }
}
