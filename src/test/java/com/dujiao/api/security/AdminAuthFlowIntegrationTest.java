package com.dujiao.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @Test
    void adminTokenCanAccessProtectedAdminEndpoint() throws Exception {
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
        Claims claims = jwtService.parse(token);
        org.junit.jupiter.api.Assertions.assertEquals("admin", claims.get("typ", String.class));

        mockMvc.perform(get("/api/v1/admin/users?page=1&page_size=1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(0));
    }
}
