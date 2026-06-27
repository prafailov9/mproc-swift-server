package com.ntros.mprocswift.service.currency.audit;

import com.ntros.mprocswift.model.currency.FxLeg;
import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.transactions.Transaction;

import java.util.List;

public interface FxLegService {
  List<FxLeg> getAllLegsOrdered();

  List<FxLeg> getAllLegsForTxnOrdered(Transaction transaction);

  List<FxLeg> getAllLegsForQuoteOrdered(FxQuote fxQuote);
  FxLeg createFxLeg(FxLeg fxLeg);
  List<FxLeg> createAllFxLegs(List<FxLeg> fxLegs);

}
