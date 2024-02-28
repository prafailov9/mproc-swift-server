package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoneyTransferRepository extends JpaRepository<MoneyTransfer, Integer> {

    @Query(value = """
            SELECT mt.*
            FROM money_transfer mt
            JOIN account sa ON mt.sender_account_id = sa.account_id
            JOIN account ra ON mt.receiver_account_id = ra.account_id
            JOIN account_details sad ON sa.account_details_id=sad.account_details_id
            JOIN account_details rad ON ra.account_details_id=rad.account_details_id
            WHERE
            sad.account_number= :accountNumber OR rad.account_number = :accountNumber""", nativeQuery = true)
    List<MoneyTransfer> findAllByAccount(@Param("accountNumber") String accountNumber);

    @Query(value = """
            SELECT mt.*
            FROM money_transfer mt
            JOIN account a ON mt.sender_account_id=a.account_id
            JOIN account_details ad ON a.account_id=ad.account_id
            WHERE
            ad.account_number= :accountNumber""", nativeQuery = true)
    List<MoneyTransfer> findAllWithdrawsByAccount(@Param("accountNumber") String accountNumber);

    @Query(value = """
            SELECT mt.*
            FROM money_transfer mt
            JOIN account a ON mt.receiver_account_id=a.account_id
            JOIN account_details ad ON a.account_id=ad.account_id
            WHERE
            ad.account_number= :accountNumber""", nativeQuery = true)
    List<MoneyTransfer> findAllReceivedByAccount(@Param("accountNumber") String accountNumber);
}
