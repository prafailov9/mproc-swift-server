package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class MoneyTransferDataService implements MoneyTransferService {

  private final MoneyTransferRepository moneyTransferRepository;

  @Autowired
  public MoneyTransferDataService(final MoneyTransferRepository moneyTransferRepository) {
    this.moneyTransferRepository = moneyTransferRepository;
  }

  @Override
  public List<MoneyTransfer> getAllTransfersForAccount(String accountNumber) {
    return moneyTransferRepository.findAllByAccount(accountNumber);
  }

  @Override
  public List<MoneyTransfer> getAllWithdrawTransfersForAccount(String accountNumber) {
    return moneyTransferRepository.findAllWithdrawsByAccount(accountNumber);
  }

  @Override
  public List<MoneyTransfer> getAllReceivedTransfersForAccount(String accountNumber) {
    return moneyTransferRepository.findAllReceivedByAccount(accountNumber);
  }
}
