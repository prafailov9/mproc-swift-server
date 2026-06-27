package com.ntros.mprocswift.dto.quotes;

import com.ntros.mprocswift.model.currency.MoneyConverter;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionLeg;
import lombok.Data;

@Data
public class FxLegDto {

  private String inputCurrency;
  private String outputCurrency;
  private String appliedRate;
  private String inputAmount;
  private String outputAmount;

  public FxLegDto(ConversionLeg conversionLeg) {
    inputCurrency = conversionLeg.inputCurrency().getCurrencyCode();
    outputCurrency = conversionLeg.outputCurrency().getCurrencyCode();

    inputAmount =
        String.valueOf(
            MoneyConverter.toMajor(
                conversionLeg.inputAmount(), conversionLeg.inputCurrency().getExponent()));
    outputAmount =
        String.valueOf(
            MoneyConverter.toMajor(
                conversionLeg.outputAmount(), conversionLeg.outputCurrency().getExponent()));

    appliedRate = String.valueOf(conversionLeg.appliedRate());
  }
}
