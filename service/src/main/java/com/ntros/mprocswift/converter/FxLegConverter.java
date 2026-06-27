package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.model.currency.FxLeg;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionLeg;
import org.springframework.stereotype.Component;

@Component
public class FxLegConverter implements Converter<ConversionLeg, FxLeg> {
  @Override
  public ConversionLeg toDto(FxLeg model) {
    return new ConversionLeg(
        model.getInputCurrency(),
        model.getOutputCurrency(),
        model.getAppliedRate(),
        model.getInputAmount(),
        model.getOutputAmount());
  }

  // callers are responsible for setting the sequence
  @Override
  public FxLeg toModel(ConversionLeg dto) {
    var fxLeg = new FxLeg();
    //        fxLeg.setFxQuote(fxQuote);
    fxLeg.setAppliedRate(dto.appliedRate());
    //        fxLeg.setSequence(seq.incrementAndGet());
    fxLeg.setInputCurrency(dto.inputCurrency());
    fxLeg.setInputAmount(dto.inputAmount());
    fxLeg.setOutputCurrency(dto.outputCurrency());
    fxLeg.setOutputAmount(dto.outputAmount());
    return fxLeg;
  }
}
