package com.kharcha.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;

    @BeforeEach void clean() { users.deleteAll(); }

    @Test
    void getsAndUpdatesOnlyTheAuthenticatedUsersAgentCap() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("settings@example.com", "password12345"))))
                .andExpect(status().isCreated());
        String body = objectMapper.writeValueAsString(new LoginRequest("settings@example.com", "password12345"));
        String token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
        mockMvc.perform(get("/api/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("settings@example.com"))
                .andExpect(jsonPath("$.agentCapPaise").value(200000));
        mockMvc.perform(put("/api/settings/agent-cap").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amountPaise\":50000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agentCapPaise").value(50000));
        mockMvc.perform(put("/api/settings/agent-cap").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amountPaise\":49999}"))
                .andExpect(status().isBadRequest());
    }
}
