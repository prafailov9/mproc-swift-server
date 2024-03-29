package com.ntros.mprocswift.repository.account;

import com.ntros.mprocswift.model.account.AccountDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDetailsRepository extends JpaRepository<AccountDetails, Integer> {
}
