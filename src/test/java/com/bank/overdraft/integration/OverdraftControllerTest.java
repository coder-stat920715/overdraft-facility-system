package com.bank.overdraft.integration;

import com.bank.overdraft.repository.DailyInterestLedgerRepository;
import com.bank.overdraft.repository.OverdraftAccountRepository;
import com.bank.overdraft.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OverdraftControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OverdraftAccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired DailyInterestLedgerRepository ledgerRepository;

    @BeforeEach
    void cleanDatabase() {
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private String openAccount() throws Exception {
        String body = """
                {"customerId":"CUST-1001","bankName":"HDFC","sanctionedLimit":1500000.00,
                 "annualInterestRate":12.00,"billingCycleDay":1}
                """;
        String response = mockMvc.perform(post("/api/v1/overdraft/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.utilizedAmount").value(0.0))
                .andExpect(jsonPath("$.availableLimit").value(1500000.0))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accountNumber").asText();
    }

    @Test
    void fullLifecycleThroughRestApi() throws Exception {
        String accountNumber = openAccount();
        String base = "/api/v1/overdraft/accounts/" + accountNumber;

        mockMvc.perform(post(base + "/drawdown").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":300000.00,\"referenceId\":\"DD-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("DRAWDOWN"))
                .andExpect(jsonPath("$.utilizedBalanceAfter").value(300000.0))
                .andExpect(jsonPath("$.availableLimitAfter").value(1200000.0));

        mockMvc.perform(post(base + "/repay").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("REPAYMENT"))
                .andExpect(jsonPath("$.utilizedBalanceAfter").value(200000.0));

        mockMvc.perform(post("/api/v1/overdraft/admin/trigger-eod-accrual").param("businessDate", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountsProcessed").value(1))
                .andExpect(jsonPath("$.totalAmount").value(65.7534));

        mockMvc.perform(get(base + "/statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.utilizedAmount").value(200000.0))
                .andExpect(jsonPath("$.utilization.utilizationPercent").value(13.33))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].transactionType").value("REPAYMENT"))
                .andExpect(jsonPath("$.dailyInterestLog.length()").value(1))
                .andExpect(jsonPath("$.dailyInterestLog[0].accruedAmount").value(65.7534));

        mockMvc.perform(post("/api/v1/overdraft/admin/trigger-monthly-billing").param("businessDate", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountsProcessed").value(1))
                .andExpect(jsonPath("$.totalAmount").value(65.75));
    }

    @Test
    void drawdownAboveLimitReturns422() throws Exception {
        String accountNumber = openAccount();
        mockMvc.perform(post("/api/v1/overdraft/accounts/" + accountNumber + "/drawdown")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1500000.01}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_LIMIT"));
    }

    @Test
    void duplicateReferenceReturns409() throws Exception {
        String accountNumber = openAccount();
        String url = "/api/v1/overdraft/accounts/" + accountNumber + "/drawdown";
        String body = "{\"amount\":1000.00,\"referenceId\":\"SAME-REF\"}";
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TRANSACTION"));
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/overdraft/accounts/DOESNOTEXIST/statement"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void invalidOpenRequestReturns400WithViolations() throws Exception {
        mockMvc.perform(post("/api/v1/overdraft/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"sanctionedLimit\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations").isNotEmpty());
    }

    @Test
    void compareEndpointReturnsSavings() throws Exception {
        mockMvc.perform(get("/api/v1/overdraft/analytics/compare")
                        .param("sanctionedLimit", "1500000").param("utilizedAmount", "300000")
                        .param("interestRate", "12").param("holdingDays", "360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cheaperOption").value("OVERDRAFT"))
                .andExpect(jsonPath("$.overdraft.totalInterest").value(35506.85))
                .andExpect(jsonPath("$.netSavings").isNumber());
    }

    @Test
    void compareEndpointRejectsInvalidParameters() throws Exception {
        mockMvc.perform(get("/api/v1/overdraft/analytics/compare")
                        .param("sanctionedLimit", "1500000").param("utilizedAmount", "300000")
                        .param("interestRate", "12").param("holdingDays", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/overdraft/analytics/compare")
                        .param("sanctionedLimit", "100000").param("utilizedAmount", "300000")
                        .param("interestRate", "12").param("holdingDays", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
