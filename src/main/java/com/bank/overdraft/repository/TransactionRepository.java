package com.bank.overdraft.repository;

import com.bank.overdraft.domain.Transaction;
import com.bank.overdraft.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId, Pageable pageable);

    List<Transaction> findByAccountIdAndTransactionType(Long accountId, TransactionType type);

    boolean existsByAccountIdAndReferenceId(Long accountId, String referenceId);
}
