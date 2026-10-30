package com.bank.overdraft.dto;

import com.bank.overdraft.enums.BankType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request to open a new overdraft facility")
public record OpenAccountRequest(
        @Schema(example = "CUST-1001")
        @NotBlank @Size(max = 64) String customerId,

        @Schema(example = "HDFC")
        @NotNull BankType bankName,

        @Schema(example = "1500000.00", description = "Sanctioned OD limit in INR")
        @NotNull @DecimalMin(value = "1.00") @Digits(integer = 15, fraction = 2) BigDecimal sanctionedLimit,

        @Schema(example = "12.00", description = "Annual interest rate in percent")
        @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "100.00") @Digits(integer = 3, fraction = 4) BigDecimal annualInterestRate,

        @Schema(example = "1", description = "Day of month (1-28) on which interest is billed; defaults to 1")
        @Min(1) @Max(28) Integer billingCycleDay) {
}
