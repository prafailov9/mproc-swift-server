package com.ntros.mprocswift.repository;

import com.ntros.mprocswift.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
    Optional<Wallet> findByIdForUpdate(@Param("walletId") Integer walletId);

    @Query(value = "SELECT * FROM wallet w WHERE w.currency_id=:currencyId", nativeQuery = true)
    List<Wallet> findAllByCurrencyId(@Param("currencyId") int currencyId);

    @Query(value = "SELECT * FROM wallet WHERE account_id = :account_id", nativeQuery = true)
    List<Wallet> findAllByAccount(@Param(value = "account_id") int accountId);

    @Query(value = "SELECT w.* FROM wallet w " +
            "JOIN currency c ON w.currency_id=c.currency_id " +
            "WHERE c.currency_name= :currencyName AND w.account_id= :accountId", nativeQuery = true)
    Optional<Wallet> findByCurrencyNameAccountId(@Param("currencyName") String currencyName, @Param("accountId") int accountId);

    @Query(value = "SELECT w.* FROM wallet w " +
            "JOIN currency c ON w.currency_id=c.currency_id " +
            "WHERE c.currency_code= :currencyCode AND w.account_id= :accountId", nativeQuery = true)
    Optional<Wallet> findByCurrencyCodeAccountId(@Param("currencyCode") String currencyCode, @Param("accountId") int accountId);

    @Query(value = """
            SELECT w.* FROM wallet w
            JOIN currency c ON w.currency_id=c.currency_id
            JOIN account a ON w.account_id=a.account_id
            JOIN account_details ad ON a.account_details_id=ad.account_details_id
            WHERE c.currency_code = :currencyCode AND ad.account_number = :accountNumber""", nativeQuery = true)
    Optional<Wallet> findByCurrencyCodeAccountNumber(@Param("currencyCode") String currencyCode, @Param("accountNumber") String accountNumber);

    @Query(value = "SELECT w.* FROM wallet w " +
            "JOIN currency c ON w.currency_id = c.currency_id " +
            "WHERE c.currency_name= :currencyName", nativeQuery = true)
    List<Wallet> findAllByCurrencyName(@Param("currencyName") String currencyName);

    @Query(value = "SELECT w.* FROM wallet w " +
            "JOIN currency c ON w.currency_id = c.currency_id " +
            "WHERE c.currency_code= :currencyCode", nativeQuery = true)
    List<Wallet> findAllByCurrencyCode(@Param("currencyCode") String currencyCode);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE w FROM wallet w
            JOIN currency c ON w.currency_id = c.currency_id
            JOIN account a ON w.account_id = a.account_id
            JOIN account_details ad ON a.account_details_id = ad.account_details_id
            WHERE c.currency_code = ?1 AND ad.account_number = ?2""", nativeQuery = true)
    int deleteByCurrencyCodeAccountNumber(String currencyCode, String accountNumber);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Wallet w SET balance = :balance WHERE w.walletId = :walletId")
    int updateBalance(@Param("walletId") int walletId, @Param("balance") BigDecimal balance);
}
