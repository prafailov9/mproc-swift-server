package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.AccountDTO;
import com.ntros.mprocswift.dto.AccountWalletCountDTO;
import com.ntros.mprocswift.dto.WalletDTO;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.account.AccountDetails;
import com.ntros.mprocswift.repository.account.AccountDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AccountConverter implements Converter<AccountDTO, Account> {

    private final WalletConverter walletModelConverter;
    private final AccountDetailsRepository accountDetailsRepository;

    @Autowired
    public AccountConverter(final WalletConverter walletModelConverter,
                            final AccountDetailsRepository accountDetailsRepository) {
        this.walletModelConverter = walletModelConverter;
        this.accountDetailsRepository = accountDetailsRepository;
    }

    @Override
    public AccountDTO toDTO(Account model) {
        AccountDTO form = new AccountDTO();
        form.setAccountNumber(model.getAccountDetails().getAccountNumber());
        form.setAccountOwner(String.format("%s %s", model.getUser().getFirstName(), model.getUser().getLastName()));
        form.setAccountNumber(model.getAccountDetails().getAccountNumber());
        form.setRoutingNumber(model.getAccountDetails().getRoutingNumber());
        form.setIban(model.getAccountDetails().getIban());
        form.setTotalBalance(model.getTotalBalance());
        form.setBicswift(model.getAccountDetails().getBicswift());
        form.setBankAddress(model.getAccountDetails().getBankAddress());
        form.setCreatedDate(model.getCreatedDate());

        List<WalletDTO> walletDTOS = model.getWallets().stream().map(walletModelConverter::toDTO).toList();
        form.setWallets(walletDTOS);
        return form;
    }

    @Override
    public Account toModel(AccountDTO accountDTO) {
        AccountDetails accountDetails = new AccountDetails();
        accountDetails.setAccountNumber(accountDTO.getAccountNumber());
        accountDetails.setRoutingNumber(accountDTO.getRoutingNumber());
        accountDetails.setIban(accountDTO.getIban());
        accountDetails.setBicswift(accountDTO.getBicswift());
        accountDetails = accountDetailsRepository.save(accountDetails);

        Account account = new Account();
        account.setAccountDetails(accountDetails);
        account.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
        return account;
    }

    public AccountWalletCountDTO toAccountWalletCountDTO(Account account) {
        AccountWalletCountDTO accountWalletCountDTO = new AccountWalletCountDTO();
        accountWalletCountDTO.setAccountDTO(toDTO(account));
        accountWalletCountDTO.setWalletCount(account.getWallets().size());
        return accountWalletCountDTO;
    }
}
