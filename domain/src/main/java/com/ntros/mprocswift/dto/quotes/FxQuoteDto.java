package com.ntros.mprocswift.dto.quotes;

import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.model.currency.MoneyConverter;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionQuote;
import lombok.Data;

import java.util.List;

@Data
public class FxQuoteDto {
  private MoneyDto sourceMoney;
  private MoneyDto targetMoney;
  private String finalRate;
  private List<FxLegDto> fxLegs;

  public FxQuoteDto(ConversionQuote quote) {
    sourceMoney =
        new MoneyDto(
            String.valueOf(
                MoneyConverter.toMajor(
                    quote.sourceMoney().minorAmount(),
                    quote.sourceMoney().currency().getExponent())),
            quote.sourceMoney().currency().getCurrencyCode());
    targetMoney =
        new MoneyDto(
            String.valueOf(
                MoneyConverter.toMajor(
                    quote.targetMoney().minorAmount(),
                    quote.targetMoney().currency().getExponent())),
            quote.targetMoney().currency().getCurrencyCode());
    finalRate = quote.effectiveRate().toString();
    fxLegs = quote.legs().stream().map(FxLegDto::new).toList();
  }

  public FxQuoteDto(MoneyDto sourceMoney, MoneyDto targetMoney, ConversionQuote quote) {
    this.sourceMoney = sourceMoney;
    this.targetMoney = targetMoney;
    finalRate = quote.effectiveRate().toString();
    fxLegs = quote.legs().stream().map(FxLegDto::new).toList();
  }
}
