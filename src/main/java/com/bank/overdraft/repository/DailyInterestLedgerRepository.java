package com.bank.overdraft.repository;

import com.bank.overdraft.domain.DailyInterestLedger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DailyInterestLedgerRepository extends JpaRepository<DailyInterestLedger, Long> {

    boolean existsByAccountIdAndAccrualDate(Long accountId, LocalDate accrualDate);

    List<DailyInterestLedger> findByAccountId(Long accountId, Pageable pageable);

    List<DailyInterestLedger> findByAccountIdOrderByAccrualDateAsc(Long accountId);

    /** Returns null when there is no unbilled row at all. */
    @Query("select sum(l.accruedAmount) from DailyInterestLedger l "
            + "where l.account.id = :accountId and l.billed = false and l.accrualDate <= :cutoff")
    BigDecimal sumUnbilledInterest(@Param("accountId") Long accountId, @Param("cutoff") LocalDate cutoff);

    @Modifying(flushAutomatically = true)
    @Query("update DailyInterestLedger l set l.billed = true "
            + "where l.account.id = :accountId and l.billed = false and l.accrualDate <= :cutoff")
    int markBilled(@Param("accountId") Long accountId, @Param("cutoff") LocalDate cutoff);

    @Query("select distinct l.account.id from DailyInterestLedger l "
            + "where l.billed = false and l.accrualDate <= :cutoff")
    List<Long> findAccountIdsWithUnbilledInterest(@Param("cutoff") LocalDate cutoff);

    @Query("select distinct l.account.id from DailyInterestLedger l "
            + "where l.billed = false and l.accrualDate <= :cutoff and l.account.billingCycleDay = :cycleDay")
    List<Long> findAccountIdsWithUnbilledInterestForCycleDay(@Param("cutoff") LocalDate cutoff,
                                                             @Param("cycleDay") int cycleDay);
}
