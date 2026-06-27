package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.ExternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.ExternalTransferResponse;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.currency.MoneyMovement;
import com.ntros.mprocswift.model.currency.RatedMoneyMovement;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class ExternalTransferAsyncService
    extends AbstractTransferAsyncService<
        ExternalTransferRequest, ExternalTransferResponse, Account> {

  @Override
  protected CompletableFuture<Account> getSender(ExternalTransferRequest transferRequest) {
    return null;
  }

  @Override
  protected CompletableFuture<Account> getReceiver(ExternalTransferRequest transferRequest) {
    return null;
  }

  @Override
  protected RatedMoneyMovement performTransfer(Account sender, Account receiver, ExternalTransferRequest transferRequest) {
    return null;
  }

  @Override
  protected void createTransferTransaction(Account sender, Account receiver, ExternalTransferRequest transferRequest, RatedMoneyMovement ratedMoneyMovement) {

  }

  @Override
  protected ExternalTransferResponse buildTransferResponse(ExternalTransferRequest transferRequest) {
    return null;
  }

}
