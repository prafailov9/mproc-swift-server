package com.ntros.mprocswift.service.merchant;

import com.ntros.mprocswift.dto.cardpayment.CardPaymentRequest;
import com.ntros.mprocswift.model.Merchant;

import java.util.concurrent.CompletableFuture;

public interface MerchantService {

    Merchant createMerchant(CardPaymentRequest cardPaymentRequest);
    CompletableFuture<Merchant> createMerchant(Merchant merchant);

    CompletableFuture<Merchant> getMerchantByName(String merchantName);


}
