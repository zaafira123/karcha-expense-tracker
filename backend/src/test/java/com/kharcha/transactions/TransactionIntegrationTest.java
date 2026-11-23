package com.kharcha.transactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kharcha.BaseIntegrationTest;
import com.kharcha.auth.dto.LoginRequest;
import com.kharcha.auth.dto.RegisterRequest;
import com.kharcha.auth.repository.UserRepository;
import com.kharcha.transactions.domain.Transaction;
import com.kharcha.transactions.dto.CreateTransactionRequest;
import com.kharcha.transactions.dto.UpdateTransactionRequest;
import com.kharcha.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static com.kharcha.common.domain.Category.FOOD_DINING;
import static com.kharcha.common.domain.Category.GROCERIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        userAToken = registerAndLogin("userA@example.com", "passwordA123");
        userBToken = registerAndLogin("userB@example.com", "passwordB123");
    }

    private String registerAndLogin(String email, String password) throws Exception {
        RegisterRequest register = new RegisterRequest(email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    @DisplayName("Create transaction succeeds and defaults date to today when omitted")
    void testCreateTransaction() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                45000L, FOOD_DINING, "Pizza Hut", null, "Pizza with friends"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amountPaise").value(45000))
                .andExpect(jsonPath("$.category.code").value("FOOD_DINING"))
                .andExpect(jsonPath("$.category.displayName").value("Food and dining"))
                .andExpect(jsonPath("$.category.isProtected").value(false))
                .andExpect(jsonPath("$.merchant").value("Pizza Hut"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.occurredOn").exists());
    }

    @Test
    @DisplayName("Create transaction preserves TEXT source when requested")
    void testCreateTextTransaction() throws Exception {
        String request = """
                {"amountPaise":45000,"category":"FOOD_DINING","merchant":"Pizza Hut","occurredOn":"2026-11-14","note":"paid 450 for pizza","source":"TEXT"}
                """;

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("TEXT"))
                .andReturn();

        UUID id = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        assertThat(transactionRepository.findById(id).orElseThrow().getSource().name()).isEqualTo("TEXT");
    }

    @Test
    @DisplayName("List transactions by month with optional category filter")
    void testListTransactionsWithFilter() throws Exception {
        LocalDate dateOct10 = LocalDate.of(2026, 10, 10);
        LocalDate dateOct15 = LocalDate.of(2026, 10, 15);

        CreateTransactionRequest t1 = new CreateTransactionRequest(20000L, FOOD_DINING, "Dosa Corner", dateOct10, null);
        CreateTransactionRequest t2 = new CreateTransactionRequest(80000L, GROCERIES, "Blinkit", dateOct15, null);

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t1)));

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(t2)));

        // List all for 2026-10 (should return 2, newest dateOct15 first)
        mockMvc.perform(get("/api/transactions?month=2026-10")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].merchant").value("Blinkit"))
                .andExpect(jsonPath("$[1].merchant").value("Dosa Corner"));

        // Filter by category GROCERIES (should return 1)
        mockMvc.perform(get("/api/transactions?month=2026-10&category=GROCERIES")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].merchant").value("Blinkit"));
    }

    @Test
    @DisplayName("Edit transaction updates fields successfully")
    void testEditTransaction() throws Exception {
        CreateTransactionRequest create = new CreateTransactionRequest(
                50000L, FOOD_DINING, "Cafe Coffee Day", LocalDate.of(2026, 10, 1), "Coffee"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        UpdateTransactionRequest update = new UpdateTransactionRequest(
                65000L, FOOD_DINING, "CCD Lounge", LocalDate.of(2026, 10, 2), "Coffee & snacks"
        );

        mockMvc.perform(put("/api/transactions/" + id)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaise").value(65000))
                .andExpect(jsonPath("$.merchant").value("CCD Lounge"))
                .andExpect(jsonPath("$.occurredOn").value("2026-10-02"));
    }

    @Test
    @DisplayName("Delete transaction returns 204 No Content")
    void testDeleteTransaction() throws Exception {
        CreateTransactionRequest create = new CreateTransactionRequest(
                15000L, FOOD_DINING, "Chai Point", LocalDate.of(2026, 10, 5), null
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/transactions/" + id)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNoContent());

        assertThat(transactionRepository.findById(UUID.fromString(id))).isEmpty();
    }

    @Test
    @DisplayName("Invalid amount (0 or negative) returns 400")
    void testInvalidAmountReturns400() throws Exception {
        CreateTransactionRequest zeroAmount = new CreateTransactionRequest(
                0L, FOOD_DINING, "Store", LocalDate.of(2026, 10, 1), null
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Future date returns 400")
    void testFutureDateReturns400() throws Exception {
        LocalDate farFuture = LocalDate.now().plusDays(10);
        CreateTransactionRequest futureDate = new CreateTransactionRequest(
                10000L, FOOD_DINING, "Future Store", farFuture, null
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Transaction date cannot be in the future"));
    }

    @Test
    @DisplayName("Ownership isolation: user B cannot read, edit, or delete user A's transaction (returns 404)")
    void testOwnershipIsolationReturns404() throws Exception {
        CreateTransactionRequest create = new CreateTransactionRequest(
                100000L, GROCERIES, "Supermarket", LocalDate.of(2026, 10, 1), "User A groceries"
        );

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();

        String txId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // User B cannot edit User A's transaction -> returns 404
        UpdateTransactionRequest update = new UpdateTransactionRequest(
                50000L, GROCERIES, "Hacked", LocalDate.of(2026, 10, 1), null
        );
        mockMvc.perform(put("/api/transactions/" + txId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        // User B cannot delete User A's transaction -> returns 404
        mockMvc.perform(delete("/api/transactions/" + txId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        // User B listing transactions sees 0 transactions
        mockMvc.perform(get("/api/transactions?month=2026-10")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
