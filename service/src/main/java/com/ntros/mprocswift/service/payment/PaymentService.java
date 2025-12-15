package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentResponse;
import com.ntros.mprocswift.dto.cardpayment.HoldSettlementRequest;
import com.ntros.mprocswift.dto.cardpayment.HoldSettlementResponse;

public interface PaymentService {

  AuthorizePaymentResponse authorizePayment(final AuthorizePaymentRequest authorizePaymentRequest);

  HoldSettlementResponse settlePayment(final HoldSettlementRequest holdSettlementRequest);
}
