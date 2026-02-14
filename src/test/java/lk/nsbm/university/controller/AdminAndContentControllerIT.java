package lk.nsbm.university.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AdminAndContentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "ADMIN01", roles = {"ADMIN"})
    void adminUsersPageLoads() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "ADMIN01", roles = {"ADMIN"})
    void contentPageLoads() throws Exception {
        mockMvc.perform(get("/content"))
                .andExpect(status().isOk());
    }
}
