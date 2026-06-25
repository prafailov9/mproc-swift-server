package com.ntros.mprocswift.service.transfer.synch;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.RatedMoneyMovement;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import com.ntros.mprocswift.repository.transaction.*;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.idempotency.IdempotencyRecordService;
import com.ntros.mprocswift.service.ledger.LedgerAccountBalanceService;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.wallet.WalletService;
import com.ntros.mprocswift.utils.TransferRequestHasher;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractTransferService<T extends TransferRequest, R extends TransferResponse>
    implements SyncTransferService<T, R> {

  @Autowired protected TransactionRepository transactionRepository;
  @Autowired protected TransactionTypeRepository transactionTypeRepository;
  @Autowired protected TransactionStatusRepository transactionStatusRepository;
  @Autowired protected MoneyTransferRepository moneyTransferRepository;
  @Autowired protected CurrencyRepository currencyRepository;

  @Autowired protected WalletService walletService;
  @Autowired protected CurrencyExchangeRateService currencyExchangeRateService;

  @Autowired protected LedgerAccountService ledgerAccountService;
  @Autowired protected LedgerEntryService ledgerEntryService;
  @Autowired protected LedgerAccountBalanceService ledgerAccountBalanceService;
  @Autowired protected IdempotencyRecordService idempotencyRecordService;

  @Transactional
  public R transfer(T request) {
    // idempotency check. Try to insert. if hit constraint, select existing and rebuild response

//    String key = request.getRequestId();
//    String hash = TransferRequestHasher.payloadHash(request);
    // claiming/loading the key must run in their own txn. Otherwise on "tryClaim()" failure, the
    // subsequent
    // "load()" will select the key that is still in-memory, in the shared transaction, even though
    // it is not actually in the database.
    //    if (!idempotencyRecordService.tryClaim(key, hash)) {
    //      return rebuildResponse(idempotencyRecordService.load(key), request); // fresh, committed
    // read
    //    }
    TransferPlan plan = planAndLock(request); // does FOR UPDATE reads
    apply(plan); // updates + inserts + ledger
    //    idempotencyRecordService.markCompleted(key, plan.transaction().getTransactionId());
    return buildResponse(request, plan);
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
      RatedMoneyMovement ratedMoneyMovement) {}
}
