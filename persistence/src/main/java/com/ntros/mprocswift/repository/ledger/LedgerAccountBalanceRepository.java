package com.ntros.mprocswift.repository.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;
import jakarta.persistence.LockModeType;

import java.util.List;
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

  @Query(
      """
    select case when count(lab) > 0 then true else false end
    from LedgerAccountBalance lab
    join lab.ledgerAccount la
    where la.wallet.walletId = :walletId
      and la.ledgerAccountType.typeCode = 'WALLET_AVAILABLE'
      and lab.balanceMinor >= :amount
    """)
  boolean hasAvailableFunds(
      @Param("walletId") int walletId, @Param("amount") long amount);

  @Query(
"""
        SELECT lab FROM LedgerAccountBalance lab
        JOIN lab.ledgerAccount la
        WHERE la.ledgerAccountType.typeCode = 'WALLET_HELD'
        """)
  List<LedgerAccountBalance> findAllHeld();

  @Query(
          """
                  SELECT lab FROM LedgerAccountBalance lab
                  JOIN lab.ledgerAccount la
                  WHERE la.ledgerAccountType.typeCode = 'WALLET_AVAILABLE'
                  """)
  List<LedgerAccountBalance> findAllAvailable();
}
