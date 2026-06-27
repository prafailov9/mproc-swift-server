package com.ntros.mprocswift.service.currency.audit;

import com.ntros.mprocswift.exceptions.EntityCreateFailedException;
import com.ntros.mprocswift.model.currency.FxLeg;
import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.currency.FxLegRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FxLegDataService implements FxLegService {

  private final FxLegRepository fxLegRepository;

  @Autowired
  public FxLegDataService(FxLegRepository fxLegRepository) {
    this.fxLegRepository = fxLegRepository;
  }

  @Override
  public List<FxLeg> getAllLegsOrdered() {
    return fxLegRepository.findAllOrderBySequence();
  }

  @Override
  public List<FxLeg> getAllLegsForTxnOrdered(Transaction transaction) {
    return fxLegRepository.findAllByTransactionOrdered(transaction);
  }

  @Override
  public List<FxLeg> getAllLegsForQuoteOrdered(FxQuote fxQuote) {
    return fxLegRepository.findAllByFxQuoteOrdered(fxQuote);
  }

  @Override
  public FxLeg createFxLeg(FxLeg fxLeg) {
    try {
      return fxLegRepository.save(fxLeg);
    } catch (DataIntegrityViolationException ex) {
      String err = String.format("Could not create fx_leg: %s. Error: %s", fxLeg, ex.getMessage());
      log.error(err);
      throw new EntityCreateFailedException(err, ex);
    }
  }

  @Override
  public List<FxLeg> createAllFxLegs(List<FxLeg> fxLegs) {
    try {
      return fxLegRepository.saveAll(fxLegs);
    } catch (DataIntegrityViolationException ex) {
      String err = String.format("Could not batch create fx_legs. Error: %s", ex.getMessage());
      log.error(err);
      throw new EntityCreateFailedException(err, ex);
    }
  }
}
