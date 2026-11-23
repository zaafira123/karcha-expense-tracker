package com.kharcha.parsing.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SmsImportIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String token;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM sms_imports");
        transactionRepository.deleteAll();
        userRepository.deleteAll();
        token = registerAndLogin("sms-import@example.com");
    }

    @Test
    void requiresConsentAndParsingDoesNotPersistMessageOrTransaction() throws Exception {
        String raw = "INR 450.00 debited at Zomato on 14-11-26. UPI Ref 928271";

        mockMvc.perform(post("/api/imports/sms/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":false,\"messages\":[" + objectMapper.writeValueAsString(raw) + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSENT_REQUIRED"));

        MvcResult result = mockMvc.perform(post("/api/imports/sms/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"messages\":[" + objectMapper.writeValueAsString(raw) + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.drafts[0].amountPaise").value(45_000))
                .andReturn();

        assertThat(transactionRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM sms_imports", Integer.class)).isZero();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(raw);
    }

    @Test
    void savesReadyDebitAndSkipsItsDuplicateOnTheNextRequest() throws Exception {
        String raw = "INR 450.00 debited at Zomato on 14-11-26. UPI Ref 928271";
        JsonNode parsed = parse(raw);
        JsonNode draft = parsed.path("drafts").get(0);
        String messageHash = draft.path("messageHash").asText();
        String save = saveBody(messageHash, raw);

        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(save))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1));

        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(save))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(0))
                .andExpect(jsonPath("$.skipped[0].reason").value("DUPLICATE"));

        assertThat(transactionRepository.count()).isEqualTo(1);
        Transaction saved = transactionRepository.findAll().get(0);
        assertThat(saved.getSource().name()).isEqualTo("SMS");
        assertThat(saved.getRawInput()).isNull();
        assertThat(saved.getNote()).isNull();
        assertThat(saved.getOccurredOn()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM sms_imports WHERE user_id = ? AND message_hash = ?", Integer.class, userId, messageHash)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM sms_imports WHERE message_hash LIKE '%' || ? || '%'", Integer.class, raw)).isZero();
    }

    @Test
    void marksTheSecondMessageInOneRequestAndTheNextRequestAsDuplicate() throws Exception {
        String raw = "INR 450.00 debited at Zomato on 14-11-26. UPI Ref 928271";
        MvcResult batch = mockMvc.perform(post("/api/imports/sms/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"messages\":[" + objectMapper.writeValueAsString(raw) + "," + objectMapper.writeValueAsString(raw) + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts[0].status").value("READY"))
                .andExpect(jsonPath("$.drafts[1].status").value("DUPLICATE"))
                .andReturn();

        JsonNode batchJson = objectMapper.readTree(batch.getResponse().getContentAsString());
        String hash = batchJson.path("drafts").get(0).path("messageHash").asText();
        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"drafts\":[{\"index\":0,\"amountPaise\":45000,\"category\":\"FOOD_DINING\",\"merchant\":\"Zomato\",\"occurredOn\":\"2026-11-14\",\"type\":\"DEBIT\",\"messageHash\":\"" + hash + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1));

        mockMvc.perform(post("/api/imports/sms/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"messages\":[" + objectMapper.writeValueAsString(raw) + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts[0].status").value("DUPLICATE"));
    }

    @Test
    void skipsCreditDraftAndRequiresConsentForSave() throws Exception {
        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":false,\"drafts\":[{\"index\":0,\"amountPaise\":45000,\"category\":\"FOOD_DINING\",\"merchant\":\"Zomato\",\"occurredOn\":\"2026-11-14\",\"type\":\"CREDIT\",\"messageHash\":\"" + "a".repeat(64) + "\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSENT_REQUIRED"));

        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"drafts\":[{\"index\":0,\"amountPaise\":45000,\"category\":\"FOOD_DINING\",\"merchant\":\"Zomato\",\"occurredOn\":\"2026-11-14\",\"type\":\"CREDIT\",\"messageHash\":\"" + "a".repeat(64) + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(0))
                .andExpect(jsonPath("$.skipped[0].reason").value("Only debit drafts can be saved."));
    }

    @Test
    void rollbackLeavesNoTransactionOrSmsRowWhenLaterSaveFails() throws Exception {
        String firstHash = parse("INR 450.00 debited at Zomato on 14-11-26. UPI Ref 928271")
                .path("drafts").get(0).path("messageHash").asText();
        String secondHash = parse("INR 300.00 debited at Uber on 13-11-26. UPI Ref 928272")
                .path("drafts").get(0).path("messageHash").asText();
        String save = "{\"consentAccepted\":true,\"drafts\":[" +
                "{\"index\":0,\"amountPaise\":45000,\"category\":\"FOOD_DINING\",\"merchant\":\"Zomato\",\"occurredOn\":\"2026-11-14\",\"type\":\"DEBIT\",\"messageHash\":\"" + firstHash + "\"}," +
                "{\"index\":1,\"amountPaise\":30000,\"category\":\"TRAVEL\",\"merchant\":\"Uber\",\"occurredOn\":\"2026-11-30\",\"type\":\"DEBIT\",\"messageHash\":\"" + secondHash + "\"}]}";

        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(save))
                .andExpect(status().isBadRequest());

        assertThat(transactionRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM sms_imports", Integer.class)).isZero();
    }

    @Test
    void ownershipIsPerUserAndUserBCanImportTheSameMessage() throws Exception {
        String raw = "INR 450.00 debited at Zomato on 14-11-26. UPI Ref 928271";
        JsonNode firstDraft = parse(raw).path("drafts").get(0);
        String hash = firstDraft.path("messageHash").asText();
        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(hash, raw)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1));

        String userBToken = registerAndLogin("sms-import-b@example.com");
        mockMvc.perform(post("/api/imports/sms/parse")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"messages\":[" + objectMapper.writeValueAsString(raw) + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts[0].status").value("READY"));
        mockMvc.perform(post("/api/imports/sms/save")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(hash, raw)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1));
    }

    private JsonNode parse(String raw) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/imports/sms/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentAccepted\":true,\"messages\":[" + objectMapper.writeValueAsString(raw) + "]}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String saveBody(String hash, String raw) throws Exception {
        JsonNode draft = parse(raw).path("drafts").get(0);
        return "{\"consentAccepted\":true,\"drafts\":[{\"index\":0,\"amountPaise\":45000,\"category\":\"FOOD_DINING\",\"merchant\":\"Zomato\",\"occurredOn\":\"2026-11-14\",\"type\":\"DEBIT\",\"messageHash\":"
                + objectMapper.writeValueAsString(hash.isBlank() ? draft.path("messageHash").asText() : hash) + "}]}";
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password123"))))
                .andExpect(status().isCreated());
        userId = userRepository.findByEmail(email).orElseThrow().getId();
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
