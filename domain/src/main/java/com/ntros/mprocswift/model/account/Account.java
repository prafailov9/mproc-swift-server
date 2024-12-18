package com.ntros.mprocswift.model.account;

import com.ntros.mprocswift.model.User;
import com.ntros.mprocswift.model.Wallet;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"accountId", "user", "accountDetails"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId;


    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "account_details_id")
    private AccountDetails accountDetails;

    @Column(nullable = false)
    private BigDecimal totalBalance;

    @Column(nullable = false)
    private Timestamp createdDate;
    private Timestamp updatedDate;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "account", cascade = CascadeType.ALL)
    private List<Wallet> wallets;

    public Wallet getMainWallet() {
        if (wallets == null || wallets.isEmpty()) {
            return null;
        }

        Wallet main = wallets.stream()
                .filter(Wallet::isMain)
                .findFirst()
                .orElse(wallets.get(0));

        // set true just in case
        main.setMain(true);
        return main;
    }

}
