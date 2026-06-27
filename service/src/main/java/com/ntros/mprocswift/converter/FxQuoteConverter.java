package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.currency.Money;
import com.ntros.mprocswift.model.currency.conversion.ConversionQuote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class FxQuoteConverter implements Converter<ConversionQuote, FxQuote> {

  @Override
  public ConversionQuote toDto(FxQuote model) {
    var legs = model.getLegs();
    var sourceMoney =
        new Money(legs.getFirst().getInputAmount(), legs.getFirst().getInputCurrency());

    var targetMoney =
        new Money(legs.getLast().getOutputAmount(), legs.getLast().getOutputCurrency());

    return new ConversionQuote(sourceMoney, targetMoney, model.finalRate, new ArrayList<>());
  }

  // caller is responsible for setting the fx_legs and transaction
  @Override
  public FxQuote toModel(ConversionQuote dto) {
    FxQuote fxQuote = new FxQuote();
    fxQuote.setSourceCurrency(dto.sourceMoney().currency());
    fxQuote.setTargetCurrency(dto.targetMoney().currency());
    fxQuote.setSourceAmount(dto.sourceMoney().minorAmount());
    fxQuote.setTargetAmount(dto.targetMoney().minorAmount());

    fxQuote.setFinalRate(dto.effectiveRate());
    return fxQuote;
  }
}
