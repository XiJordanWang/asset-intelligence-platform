package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.asset.entity.AssetStatus;
import io.github.xijordanwang.assetintel.site.entity.Site;
import io.github.xijordanwang.assetintel.site.repository.SiteRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "viewer", roles = "ADMIN") // reads need a signed-in user who can see every area
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MasterDataAndExportTest {

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
        Site s = sites.save(new Site("HLZ-01", "Hamilton plant", "HLZ", "Hamilton", "Waikato", -37.78, 175.27, new BigDecimal("1000000")));
        Asset a = new Asset("AST-1", "Laptop", AssetCategory.IT_EQUIPMENT, AssetStatus.IN_SERVICE, s, "Kauri IT", new BigDecimal("3000"), LocalDate.now().minusMonths(6));
        a.assignCustodian("aroha");
        assets.save(a);
    }

    @Test
    void masterDataChangeShowsOldAndNewValuesAndAppliesAfterApproval() throws Exception {
        String body = mvc.perform(post("/api/changes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"MODIFICATION","assetTag":"AST-1","requestedBy":"ben","newName":"Engineering laptop","newCustodian":"mere"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ref = com.jayway.jsonpath.JsonPath.read(body, "$.reference");

        mvc.perform(post("/api/changes/{r}/submit", ref))
                .andExpect(jsonPath("$.fieldChanges[0].field").value("name"))
                .andExpect(jsonPath("$.fieldChanges[0].oldValue").value("Laptop"))
                .andExpect(jsonPath("$.fieldChanges[0].newValue").value("Engineering laptop"))
                .andExpect(jsonPath("$.fieldChanges[1].oldValue").value("aroha"));

        for (String[] step : new String[][]{{"PLANT_MANAGER", "tama"}, {"FINANCE_CONTROLLER", "grace"}}) {
            actor = TestUsers.as(step[1]);
            mvc.perform(post("/api/changes/{r}/approve", ref).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"role\":\"%s\",\"decidedBy\":\"%s\"}".formatted(step[0], step[1])));
        }
        mvc.perform(get("/api/assets/AST-1"))
                .andExpect(jsonPath("$.name").value("Engineering laptop"))
                .andExpect(jsonPath("$.custodian").value("mere"))
                .andExpect(jsonPath("$.supplier").value("Kauri IT"));
    }

    @Test
    void masterDataChangeMustChangeSomething() throws Exception {
        mvc.perform(post("/api/changes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"MODIFICATION","assetTag":"AST-1","requestedBy":"ben","newName":"Laptop"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void assetRegisterExportsAsARealSpreadsheet() throws Exception {
        byte[] xlsx = mvc.perform(get("/api/export/assets.xlsx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Tag");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("AST-1");
            assertThat(sheet.getRow(1).getCell(8).getNumericCellValue()).isEqualTo(3000.0);
        }
        mvc.perform(get("/api/export/changes.xlsx")).andExpect(status().isOk());
    }
}
