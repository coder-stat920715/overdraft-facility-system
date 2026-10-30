package com.bank.overdraft.mapper;

import com.bank.overdraft.domain.DailyInterestLedger;
import com.bank.overdraft.domain.OverdraftAccount;
import com.bank.overdraft.domain.Transaction;
import com.bank.overdraft.dto.AccountResponse;
import com.bank.overdraft.dto.DailyInterestResponse;
import com.bank.overdraft.dto.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OverdraftMapper {

    AccountResponse toAccountResponse(OverdraftAccount account);

    @Mapping(target = "accountNumber", source = "account.accountNumber")
    TransactionResponse toTransactionResponse(Transaction transaction);

    DailyInterestResponse toDailyInterestResponse(DailyInterestLedger ledger);

    List<TransactionResponse> toTransactionResponses(List<Transaction> transactions);

    List<DailyInterestResponse> toDailyInterestResponses(List<DailyInterestLedger> ledgers);
}
