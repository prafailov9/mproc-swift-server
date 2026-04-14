package com.ntros.mprocswift.service.transfer.synch;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.MoneyMovement;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.ledger.Posting;
import com.ntros.mprocswift.service.wallet.WalletService;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractMoneyTransferService<
        T extends TransferRequest, R extends TransferResponse>
    implements MoneyTransferService<T, R> {

  @Autowired protected TransactionRepository transactionRepository;
  @Autowired protected TransactionTypeRepository transactionTypeRepository;
  @Autowired protected TransactionStatusRepository transactionStatusRepository;
  @Autowired protected MoneyTransferRepository moneyTransferRepository;

  @Autowired protected WalletService walletService;
  @Autowired protected CurrencyExchangeRateService currencyExchangeRateService;

  @Autowired protected LedgerAccountService ledgerAccountService;
  @Autowired protected LedgerEntryService ledgerEntryService;

  @Transactional
  public R transfer(T request) {
    validateRequest(request); // no DB, pure validation
    TransferPlan plan = planAndLock(request); // does FOR UPDATE reads
    apply(plan); // updates + inserts + ledger
    return buildResponse(request, plan);
  }

  /** This MUST do: - lock rows - compute - update - insert tx + ledger */
  @Transactional
  protected R execInTransaction(T request) {
    TransferPlan plan = planAndLock(request); // lock-dependent
    apply(plan); // update balances + persist ledger/tx
    return buildResponse(request, plan);
  }

  protected void validateRequest(T request) {
    // e.g. request.getAmount() > 0 already validated by bean validation
  }

  /** Locks what is needed and returns a full plan (all amounts in minor units). */
  protected abstract TransferPlan planAndLock(T request);

  /** Applies balance updates + transaction + ledger entries. */
  protected abstract void apply(TransferPlan plan);

  /** Build response (can include moneyMovement). */
  protected abstract R buildResponse(T request, TransferPlan plan);

  protected record TransferPlan(
      Transaction transaction,
      Wallet senderWallet,
      Wallet receiverWallet,
      MoneyMovement moneyMovement,
      List<Posting> postings) {}
}
