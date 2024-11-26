package com.ntros.mprocswift.service.merchant;

import com.ntros.mprocswift.exceptions.AccountConstraintFailureException;
import com.ntros.mprocswift.exceptions.MerchantConstraintFailureException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class MerchantDataService implements MerchantService {

    private final Executor executor;

    private final MerchantRepository merchantRepository;

    @Autowired
    public MerchantDataService(MerchantRepository merchantRepository, Executor executor) {
        this.merchantRepository = merchantRepository;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Merchant> createMerchant(Merchant merchant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return merchantRepository.save(merchant);
            } catch (DataIntegrityViolationException ex) {
                log.error("Could not save merchant {}. {}", merchant, ex.getMessage(), ex);
                throw new MerchantConstraintFailureException(merchant);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Merchant> getMerchantByName(String merchantName) {
        return CompletableFuture
                .supplyAsync(() -> merchantRepository
                        .findByMerchantName(merchantName)
                        .orElse(null));
    }
}
