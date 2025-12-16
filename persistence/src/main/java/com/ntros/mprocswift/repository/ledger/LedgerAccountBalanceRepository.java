package com.ntros.mprocswift.repository.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerAccountBalanceRepository
    extends JpaRepository<LedgerAccountBalance, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT b FROM LedgerAccountBalance b WHERE b.ledgerAccountId = :ledgerAccountId")
  Optional<LedgerAccountBalance> findByIdForUpdate(
      @Param("ledgerAccountId") Integer ledgerAccountId);

  @Modifying
  @Query(
      """
    UPDATE LedgerAccountBalance b
       SET b.balanceMinor = b.balanceMinor + :delta,
           b.updatedAt = CURRENT_TIMESTAMP
     WHERE b.ledgerAccountId = :ledgerAccountId
  """)
  int updateBalance(@Param("ledgerAccountId") Integer ledgerAccountId, @Param("delta") long delta);
}
