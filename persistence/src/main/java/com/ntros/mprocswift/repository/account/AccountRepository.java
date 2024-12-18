package com.ntros.mprocswift.repository.account;

import com.ntros.mprocswift.model.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    @Query(value = "SELECT a.* FROM account a " +
            "JOIN account_details ad ON a.account_details_id=ad.account_details_id " +
            "WHERE ad.account_number = :accountNumber", nativeQuery = true)
    Optional<Account> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT a FROM Account a " +
            "JOIN a.accountDetails ad " +
            "WHERE ad.accountName = :accountName")
    Optional<Account> findByAccountName(@Param("accountName") String accountName);

    @Query(value = "SELECT a.* FROM account a " +
            "JOIN wallet w ON a.account_id = w.account_id " +
            "GROUP BY a.account_id " +
            "HAVING COUNT(w.wallet_id) = :count", nativeQuery = true)
    List<Account> findAllByWalletCount(@Param("count") int count);

}
