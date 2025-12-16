package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.ExternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.ExternalTransferResponse;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.currency.MoneyMovement;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
  protected MoneyMovement performTransfer(
      Account sender, Account receiver, ExternalTransferRequest transferRequest) {
    return new MoneyMovement(
        1,
        sender.getMainWallet().get().getCurrency(),
        2,
        receiver.getMainWallet().get().getCurrency());
  }

  @Override
  protected void createTransferTransaction(
      Account sender,
      Account receiver,
      ExternalTransferRequest transferRequest,
      MoneyMovement moneyMovement) {}

  @Override
  protected ExternalTransferResponse buildTransferResponse(
      ExternalTransferRequest transferRequest) {
    return null;
  }
}
