package io.github.xijordanwang.assetintel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Each role sees its own areas of the app and its own to-do list on the home page. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class RoleAccessAndMyWorkTest {

    @Autowired MockMvc mvc;

    private MockHttpSession signIn(String username) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"demo\"}".formatted(username)))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void eachRoleSeesOnlyItsAreas() throws Exception {
        MockHttpSession coordinator = signIn("pita.coordinator");
        mvc.perform(get("/api/stocktakes").session(coordinator)).andExpect(status().isOk());
        mvc.perform(get("/api/assets").session(coordinator)).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/overview").session(coordinator)).andExpect(status().isForbidden());
        mvc.perform(get("/api/requests").session(coordinator)).andExpect(status().isForbidden());

        MockHttpSession requester = signIn("aroha.requester");
        mvc.perform(get("/api/requests").session(requester)).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/overview").session(requester)).andExpect(status().isForbidden());
        mvc.perform(get("/api/stocktakes").session(requester)).andExpect(status().isForbidden());

        MockHttpSession cfo = signIn("hemi.cfo");
        mvc.perform(get("/api/dashboard/overview").session(cfo)).andExpect(status().isOk());
        mvc.perform(get("/api/stocktakes").session(cfo)).andExpect(status().isForbidden());

        mvc.perform(get("/api/my-work")).andExpect(status().isUnauthorized());
    }

    @Test
    void myWorkHasTheSectionsForEachRole() throws Exception {
        mvc.perform(get("/api/my-work").session(signIn("aroha.requester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[*].key", contains("mine")));
        mvc.perform(get("/api/my-work").session(signIn("tama.plantmanager")))
                .andExpect(jsonPath("$.sections[*].key", contains("approvals", "mine")));
        mvc.perform(get("/api/my-work").session(signIn("grace.finance")))
                .andExpect(jsonPath("$.sections[*].key", contains("approvals", "finance")));
        mvc.perform(get("/api/my-work").session(signIn("pita.coordinator")))
                .andExpect(jsonPath("$.sections[*].key", contains("counts")));
        mvc.perform(get("/api/my-work").session(signIn("sam.admin")))
                .andExpect(jsonPath("$.sections[*].key", containsInAnyOrder("mine", "counts", "finance")));
    }

    @Test
    void plantManagersOnlySeeApprovalsForTheirOwnPlant() throws Exception {
        mvc.perform(get("/api/my-work").session(signIn("tama.plantmanager")))
                .andExpect(jsonPath("$.sections[0].items[*].detail",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(" · AKL-")))));
    }
}
