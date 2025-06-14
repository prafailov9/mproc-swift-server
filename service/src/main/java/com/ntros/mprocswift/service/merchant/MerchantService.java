package com.ntros.mprocswift.service.merchant;

import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.model.Merchant;

import java.util.concurrent.CompletableFuture;

public interface MerchantService {

    Merchant createMerchant(AuthorizePaymentRequest authorizePaymentRequest);
    CompletableFuture<Merchant> createMerchant(Merchant merchant);

    Merchant getMerchantByName(String merchantName);

    Merchant getOrCreateMerchant(AuthorizePaymentRequest authorizePaymentRequest);


}
