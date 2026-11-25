package io.github.xijordanwang.assetintel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class AuthAndRolesTest {

    @Autowired MockMvc mvc;

    private MockHttpSession signIn(String username, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
        return session;
    }

    @Test
    void demoUsersSignInAndSeeWhoTheyAre() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/personas"))
                .andExpect(jsonPath("$[*].username", hasItem("tama.plantmanager")))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        MockHttpSession session = signIn("tama.plantmanager", "demo");
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.plantCode").value("HLZ"))
                .andExpect(jsonPath("$.roles[0]").value("PLANT_MANAGER"));

        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void wrongPasswordIsRefused() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"grace.finance\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postsWithoutTheCsrfTokenAreRefused() throws Exception {
        MockHttpSession session = signIn("aroha.requester", "demo");
        mvc.perform(post("/api/requests").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminsManageRoles() throws Exception {
        MockHttpSession finance = signIn("grace.finance", "demo");
        mvc.perform(get("/api/users").session(finance)).andExpect(status().isForbidden());

        MockHttpSession admin = signIn("sam.admin", "demo");
        mvc.perform(get("/api/users").session(admin)).andExpect(status().isOk());
        mvc.perform(put("/api/users/{u}/roles", "aroha.requester").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"REQUESTER\",\"SITE_COORDINATOR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("SITE_COORDINATOR")));
        mvc.perform(put("/api/users/{u}/roles", "sam.admin").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"REQUESTER\"]}"))
                .andExpect(status().isConflict());
    }
}
