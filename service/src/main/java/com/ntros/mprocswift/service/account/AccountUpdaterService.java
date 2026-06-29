package com.ntros.mprocswift.service.account;

import com.ntros.mprocswift.model.account.Account;

public interface AccountUpdaterService {

  Account updateAndFetchWalletAccount(String accountNumber);
}
