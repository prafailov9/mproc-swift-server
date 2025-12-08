package com.ntros.mprocswift.model;

import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.currency.Currency;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"walletId", "account", "currency"})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer walletId;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
    private BigDecimal balance;
    private boolean isMain;

    public void increaseBalance(final BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void decreaseBalance(final BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public boolean hasAvailableBalance(final BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public boolean verifyOwnership(String accountNumber, String currencyCode) {
        return account.getAccNumber().equals(accountNumber) && this.currency.getCurrencyCode().equals(currencyCode);
    }
}
