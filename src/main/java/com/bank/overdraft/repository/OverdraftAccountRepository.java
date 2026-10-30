package com.bank.overdraft.repository;

import com.bank.overdraft.domain.OverdraftAccount;
import com.bank.overdraft.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OverdraftAccountRepository extends JpaRepository<OverdraftAccount, Long> {

    Optional<OverdraftAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from OverdraftAccount a where a.accountNumber = :accountNumber")
    Optional<OverdraftAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from OverdraftAccount a where a.id = :id")
    Optional<OverdraftAccount> findByIdForUpdate(@Param("id") Long id);

    @Query("select a.id from OverdraftAccount a where a.status = :status order by a.id")
    List<Long> findIdsByStatus(@Param("status") AccountStatus status);
}
