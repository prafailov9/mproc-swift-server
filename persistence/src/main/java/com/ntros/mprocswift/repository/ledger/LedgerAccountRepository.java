package com.ntros.mprocswift.repository.ledger;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.ExternalAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Integer> {

  @Query("SELECT la from LedgerAccount la " + "WHERE la.wallet = :wallet")
  List<LedgerAccount> findAllByWallet(Wallet wallet);

  @Query(
      """
                SELECT la
                FROM LedgerAccount la
                WHERE la.wallet IS NOT NULL
                  AND la.ledgerAccountType.ledgerAccountTypeId IN (1, 2)
            """)
  List<LedgerAccount> findAllWalletLedgerAccounts();

  @Query(
      """
                SELECT la
                FROM LedgerAccount la
                WHERE la.merchant IS NOT NULL
                  AND la.ledgerAccountType.ledgerAccountTypeId = 3
            """)
  List<LedgerAccount> findAllMerchantSettlementLedgerAccounts();

  @Query(
      """
            SELECT la
            FROM LedgerAccount la
            WHERE la.wallet = :wallet
                AND la.ledgerAccountType.typeCode = :typeCode
                AND la.currency.currencyCode = : currencyCode
            """)
  Optional<LedgerAccount> findOneByWalletLedgerTypeCurrencyCode(
      Wallet wallet, String typeCode, String currencyCode);

  @Query(
      """
            SELECT la
            FROM LedgerAccount la
            WHERE la.merchant = :merchant
                AND la.ledgerAccountType.typeCode = :typeCode
                AND la.currency.currencyCode = : currencyCode
            """)
  Optional<LedgerAccount> findOneByMerchantLedgerTypeCurrencyCode(
      Merchant merchant, String typeCode, String currencyCode);

  @Query(
      """
            SELECT la
            FROM LedgerAccount la
            WHERE la.externalAccount = :externalAccount
                AND la.ledgerAccountType.typeCode = :typeCode
                AND la.currency.currencyCode = : currencyCode
            """)
  Optional<LedgerAccount> findOneByExternalAccountLedgerTypeCurrencyCode(
      ExternalAccount externalAccount, String typeCode, String currencyCode);

  @Query(
      """
            SELECT la
            FROM LedgerAccount la
            WHERE la.wallet IS NULL
                AND la.merchant IS NULL
                AND la.externalAccount IS NULL
                AND la.ledgerAccountType.typeCode = :typeCode
                AND la.currency.currencyCode = : currencyCode
            """)
  Optional<LedgerAccount> findSystemAccount(String typeCode, String currencyCode);
}
