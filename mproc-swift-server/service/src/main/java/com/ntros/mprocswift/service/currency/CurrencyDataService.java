package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.exceptions.CurrencyNotFoundException;
import com.ntros.mprocswift.exceptions.FailedToActivateAllCurrenciesException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.repository.WalletRepository;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@Transactional
public class CurrencyDataService implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyDataService.class);
    private final CurrencyRepository currencyRepository;
    private final WalletRepository walletRepository;

    @Autowired
    public CurrencyDataService(CurrencyRepository currencyRepository, WalletRepository walletRepository) {
        this.currencyRepository = currencyRepository;
        this.walletRepository = walletRepository;
    }


    // convert an amount from one currency to another

    @Override
    public CompletableFuture<Currency> getCurrencyByCodeAsync(String code) {
        return CompletableFuture.supplyAsync(() -> currencyRepository.findByCurrencyCode(code)
                .orElseThrow(() -> new CurrencyNotFoundException(String.format("Could not find currency with code:%s", code))));
    }

    @Override
    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCurrencyCode(code)
                .orElseThrow(() -> new CurrencyNotFoundException(String.format("Could not find currency with code:%s", code)));
    }

    @Override
    public CompletableFuture<Void> activateAll() {
        return CompletableFuture.runAsync(() -> {
            try {
                currencyRepository.updateActivateAll();
                log.info("All currencies activated successfully");
            } catch (DataAccessException ex) {
                log.error("Error occurred while activating currencies: {}", ex.getMessage(), ex);
                throw new FailedToActivateAllCurrenciesException(ex.getMessage(), ex);
            }
        });
    }

    @Override
    @Transactional
    @Modifying
    public CompletableFuture<Void> deleteCurrency(int currencyId) {
        return CompletableFuture.runAsync(() ->  {
            try {
                List<Wallet> wallets = walletRepository.findAllByCurrencyId(currencyId);
                walletRepository.deleteAll(wallets);
                currencyRepository.deleteById(currencyId);
            } catch (DataAccessException ex) {
                log.error("Currency with id {} could not be deleted", currencyId, ex);
                throw new CompletionException(ex.getMessage(), ex.getCause());
            }
        });
    }
}
