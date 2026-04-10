package com.dujiao.api.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthzMeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void authzMeReturnsRolesAndPoliciesAfterAdminLogin() throws Exception {
        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/v1/admin/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status_code").value(0))
                        .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText("");

        mockMvc.perform(get("/api/v1/admin/authz/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0))
                .andExpect(jsonPath("$.data.admin_id").isNumber())
                .andExpect(jsonPath("$.data.roles[0]").isString())
                .andExpect(jsonPath("$.data.policies").isArray());
    }
}
