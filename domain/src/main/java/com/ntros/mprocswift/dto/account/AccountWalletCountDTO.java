package com.ntros.mprocswift.dto.account;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AccountWalletCountDTO {

    private int walletCount;
    private AccountDTO accountDTO;

}
