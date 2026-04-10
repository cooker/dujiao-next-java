package com.dujiao.api.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AdminAuthzControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void rolePolicyAndAdminRoleFlowWorks() throws Exception {
        String token = loginAdmin();

        mockMvc.perform(
                        post("/api/v1/admin/authz/roles")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"qa_tester\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0))
                .andExpect(jsonPath("$.data.role").value("role:qa_tester"));

        mockMvc.perform(
                        post("/api/v1/admin/authz/policies")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"role\":\"role:qa_tester\",\"object\":\"/admin/products\",\"action\":\"GET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0));

        mockMvc.perform(get("/api/v1/admin/authz/roles/role:qa_tester/policies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0))
                .andExpect(jsonPath("$.data[0].object").value("/admin/products"));

        mockMvc.perform(
                        put("/api/v1/admin/authz/admins/1/roles")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roles\":[\"role:qa_tester\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0));

        mockMvc.perform(get("/api/v1/admin/authz/admins/1/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0))
                .andExpect(jsonPath("$.data[0]").value("role:qa_tester"));

        mockMvc.perform(
                        delete("/api/v1/admin/authz/policies")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"role\":\"role:qa_tester\",\"object\":\"/admin/products\",\"action\":\"GET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0));

        mockMvc.perform(
                        put("/api/v1/admin/authz/admins/1/roles")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roles\":[\"role:system_admin\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0));
    }

    private String loginAdmin() throws Exception {
        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/v1/admin/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status_code").value(0))
                        .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return root.path("data").path("token").asText("");
    }
}
