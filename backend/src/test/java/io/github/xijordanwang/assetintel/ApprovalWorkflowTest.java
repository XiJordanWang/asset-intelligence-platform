package io.github.xijordanwang.assetintel;

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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "viewer", roles = "ADMIN") // reads need a signed-in user who can see every area
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApprovalWorkflowTest {

    @Autowired MockMvc mvc;

    /** The user the next POST is sent as. Read when the request runs, so a helper can switch it just before. */
    private org.springframework.test.web.servlet.request.RequestPostProcessor actor = TestUsers.as("aroha");

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String url, Object... vars) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url, vars)
                .with(request -> actor.postProcessRequest(request));
    }
    @Autowired SiteRepository sites;

    @BeforeEach
    void site() {
        sites.save(new Site("HLZ-01", "Test plant", "HLZ", "Hamilton", "Waikato",
                -37.78, 175.27, new BigDecimal("1000000")));
    }

    private String createDraft(String amount) throws Exception {
        String body = """
                {"title":"Laser cutter","justification":"Prototype work","category":"PLANT_MACHINERY",
                 "amount":%s,"siteCode":"HLZ-01","requestedBy":"aroha"}""".formatted(amount);
        String json = mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.reference");
    }

    private String decision(String role, String by) {
        actor = TestUsers.as(by);
        return """
                {"role":"%s","decidedBy":"%s","comment":"ok"}""".formatted(role, by);
    }

    @Test
    void requestIsApprovedInOrderThenCapitalisedIntoTheRegister() throws Exception {
        String ref = createDraft("120000");

        mvc.perform(post("/api/requests/{r}/submit", ref))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.waitingOn").value("PLANT_MANAGER"))
                .andExpect(jsonPath("$.steps.length()").value(2));

        // Finance cannot approve before the plant manager: the step belongs to someone else.
        mvc.perform(post("/api/requests/{r}/approve", ref).contentType(MediaType.APPLICATION_JSON)
                        .content(decision("FINANCE_CONTROLLER", "grace")))
                .andExpect(status().isForbidden());
        // Neither can another plant's manager.
        mvc.perform(post("/api/requests/{r}/approve", ref).contentType(MediaType.APPLICATION_JSON)
                        .content(decision("PLANT_MANAGER", "nikau")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/requests/{r}/approve", ref).contentType(MediaType.APPLICATION_JSON)
                        .content(decision("PLANT_MANAGER", "tama")))
                .andExpect(jsonPath("$.waitingOn").value("FINANCE_CONTROLLER"));
        mvc.perform(post("/api/requests/{r}/approve", ref).contentType(MediaType.APPLICATION_JSON)
                        .content(decision("FINANCE_CONTROLLER", "grace")))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/requests/{r}/capitalise", ref))
                .andExpect(jsonPath("$.status").value("CAPITALISED"))
                .andExpect(jsonPath("$.capitalisedAssetTag").value("CAP-" + ref));

        mvc.perform(get("/api/assets/{tag}", "CAP-" + ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_SERVICE"))
                .andExpect(jsonPath("$.originalValue").value(120000.0));
    }

    @Test
    void requesterCannotApproveOwnRequest() throws Exception {
        actor = TestUsers.as("tama"); // a plant manager raising their own request
        String ref = createDraft("10000");
        mvc.perform(post("/api/requests/{r}/submit", ref));
        mvc.perform(post("/api/requests/{r}/approve", ref).contentType(MediaType.APPLICATION_JSON)
                        .content(decision("PLANT_MANAGER", "tama")))
                .andExpect(status().isConflict());
    }

    @Test
    void submitIsRefusedWhenItWouldExceedTheSiteBudget() throws Exception {
        String ref = createDraft("1500000");
        mvc.perform(post("/api/requests/{r}/submit", ref))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("capex budget")));
    }

    @Test
    void rejectionEndsTheWorkflow() throws Exception {
        String ref = createDraft("10000");
        mvc.perform(post("/api/requests/{r}/submit", ref));
        mvc.perform(post("/api/requests/{r}/reject", ref).contentType(MediaType.APPLICATION_JSON)
                        .content(decision("PLANT_MANAGER", "tama")))
                .andExpect(jsonPath("$.status").value("REJECTED"));
        actor = TestUsers.as("grace");
        mvc.perform(post("/api/requests/{r}/capitalise", ref)).andExpect(status().isConflict());
    }

    @Test
    void writesNeedASignedInUserWithTheRightRole() throws Exception {
        String ref = createDraft("10000");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/requests/{r}/submit", ref)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());
        actor = TestUsers.as("kiri");
        mvc.perform(post("/api/requests/{r}/submit", ref)).andExpect(status().isForbidden());
        mvc.perform(get("/api/requests/{r}", ref)).andExpect(status().isOk());
        // Visitors who have not signed in see nothing.
        mvc.perform(get("/api/requests/{r}", ref)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidInputGivesBadRequest() throws Exception {
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"category\":\"VEHICLE\",\"amount\":0,\"siteCode\":\"HLZ-01\",\"requestedBy\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}
