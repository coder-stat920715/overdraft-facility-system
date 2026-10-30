package com.bank.overdraft.controller;

import com.bank.overdraft.dto.AccountResponse;
import com.bank.overdraft.dto.OpenAccountRequest;
import com.bank.overdraft.dto.StatementResponse;
import com.bank.overdraft.dto.TransactionRequest;
import com.bank.overdraft.dto.TransactionResponse;
import com.bank.overdraft.exception.ApiError;
import com.bank.overdraft.service.ODService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/overdraft/accounts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Overdraft Accounts", description = "Account opening, drawdown, repayment and statements")
public class OverdraftAccountController {

    private final ODService odService;

    @PostMapping
    @Operation(summary = "Open a new overdraft account")
    @ApiResponse(responseCode = "201", description = "Account opened")
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odService.openAccount(request));
    }

    @PostMapping("/{accountNumber}/drawdown")
    @Operation(summary = "Withdraw funds from the overdraft limit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Drawdown posted"),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Insufficient available limit",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TransactionResponse drawdown(@PathVariable String accountNumber,
                                        @Valid @RequestBody TransactionRequest request) {
        return odService.drawdown(accountNumber, request);
    }

    @PostMapping("/{accountNumber}/repay")
    @Operation(summary = "Repay utilized funds (partial or full, no prepayment penalty)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repayment posted"),
            @ApiResponse(responseCode = "422", description = "Repayment exceeds outstanding amount",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TransactionResponse repay(@PathVariable String accountNumber,
                                     @Valid @RequestBody TransactionRequest request) {
        return odService.repay(accountNumber, request);
    }

    @GetMapping("/{accountNumber}/statement")
    @Operation(summary = "Account statement: transactions, daily interest log and utilization breakdown")
    public StatementResponse statement(@PathVariable String accountNumber,
                                       @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return odService.getStatement(accountNumber, limit);
    }
}
