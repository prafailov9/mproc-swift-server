package com.ntros.mprocswift.service.merchant;

import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.exceptions.MerchantConstraintFailureException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.repository.MerchantRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
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
    @Transactional
    public Merchant createMerchant(AuthorizePaymentRequest authorizePaymentRequest) {
        return merchantRepository.save(new Merchant(authorizePaymentRequest.getMerchant()));
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
    public Merchant getMerchantByName(String merchantName) {
        return merchantRepository
                .findByMerchantName(merchantName)
                .orElseThrow(() -> new NotFoundException(String.format("Merchant %s not found", merchantName)));
    }

    @Override
    public Merchant getOrCreateMerchant(AuthorizePaymentRequest authorizePaymentRequest) {
        Optional<Merchant> merchantOptional = merchantRepository
                .findByMerchantName(authorizePaymentRequest.getMerchant());
        return merchantOptional.orElseGet(() -> createMerchant(authorizePaymentRequest));
    }
}
