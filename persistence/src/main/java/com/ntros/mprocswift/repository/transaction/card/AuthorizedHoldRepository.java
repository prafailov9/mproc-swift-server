package com.ntros.mprocswift.repository.transaction.card;

import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizedHoldRepository extends JpaRepository<AuthorizedHold, Integer> {

    @Query("""
            SELECT a
            FROM AuthorizedHold a
            JOIN a.cardAuthorization ca
            WHERE ca.authorizationCode = :authorizationCode
            """)
    Optional<AuthorizedHold> findOneByAuthCode(@Param("authorizationCode") String authorizationCode);

    List<AuthorizedHold> findAllByWallet(Wallet wallet);

    List<AuthorizedHold> findAllByWalletAndIsReleasedFalseAndExpiresAtAfter(
            Wallet wallet,
            OffsetDateTime now
    );

    @Query("""
       SELECT COALESCE(SUM(h.holdAmount), 0)
       FROM AuthorizedHold h
       WHERE h.wallet = :wallet
         AND h.isReleased = false
         AND h.expiresAt > :now
       """)
    BigDecimal sumActiveHoldAmount(@Param("wallet") Wallet wallet, @Param("now") OffsetDateTime now);


}
