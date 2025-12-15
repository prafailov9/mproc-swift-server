package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.ExternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.ExternalTransferResponse;
import com.ntros.mprocswift.model.account.Account;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class ExternalTransferService
    extends AbstractTransferService<ExternalTransferRequest, ExternalTransferResponse, Account> {

  @Override
  protected CompletableFuture<Account> getSender(ExternalTransferRequest transferRequest) {
    return null;
  }

  @Override
  protected CompletableFuture<Account> getReceiver(ExternalTransferRequest transferRequest) {
    return null;
  }

  @Override
  protected BigDecimal performTransfer(
      Account sender, Account receiver, ExternalTransferRequest transferRequest) {
    return BigDecimal.ONE;
  }

  @Override
  protected void createTransferTransaction(
      Account sender,
      Account receiver,
      ExternalTransferRequest transferRequest,
      TxAmounts txAmounts) {}

  @Override
  protected ExternalTransferResponse buildTransferResponse(
      ExternalTransferRequest transferRequest) {
    return null;
  }
}
