package com.bank.overdraft.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Drawdown or repayment request")
public record TransactionRequest(
        @Schema(example = "300000.00")
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,

        @Schema(example = "DD-20260301-0001", description = "Optional client idempotency reference, unique per account")
        @Size(max = 64) String referenceId,

        @Schema(example = "Working capital")
        @Size(max = 255) String description) {
}
