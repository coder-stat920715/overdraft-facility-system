package com.bank.overdraft.controller;

import com.bank.overdraft.dto.ComparisonRequest;
import com.bank.overdraft.dto.ComparisonResponse;
import com.bank.overdraft.service.ComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/overdraft/analytics")
@RequiredArgsConstructor
@Validated
@Tag(name = "Analytics", description = "Overdraft vs personal loan financial analysis")
public class AnalyticsController {

    private final ComparisonService comparisonService;

    @GetMapping("/compare")
    @Operation(summary = "Compare personal loan (lump sum EMI) vs overdraft (utilization based) interest cost")
    public ComparisonResponse compare(
            @Parameter(example = "1500000") @RequestParam @NotNull
            @DecimalMin("1.00") @Digits(integer = 15, fraction = 2) BigDecimal sanctionedLimit,
            @Parameter(example = "300000") @RequestParam @NotNull
            @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal utilizedAmount,
            @Parameter(example = "12") @RequestParam @NotNull
            @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 4) BigDecimal interestRate,
            @Parameter(example = "360") @RequestParam @NotNull @Min(1) @Max(3650) Integer holdingDays) {
        return comparisonService.compare(new ComparisonRequest(sanctionedLimit, utilizedAmount, interestRate, holdingDays));
    }
}
