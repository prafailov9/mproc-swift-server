package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.exceptions.InvalidDecimalAmountException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public final class CurrencyUtils {

    public static final List<String> BASE_CURRENCIES = List.of("USD", "EUR");
    private static final Integer WHOLE_NUMBER_MAX_DIGIT_COUNT = 20;
    private static final Integer TRAILING_NUMBER_MAX_DIGIT_COUNT = 6;

    /**
     * Method to determine the trailing digit count(scaling factor) after the decimal point.
     * Calculates the minimum scaling factor for given amount based of allowed digit counts.
     * NUMBER_MAX_DIGIT_COUNT is the max allowed digit count for the number after the decimal point.
     *
     * @param amount to scale
     * @return a valid trailing number scale for the entire number value
     */
    public static int getScale(final BigDecimal amount) {
        if (amount == null) {
            throw new InvalidDecimalAmountException();
        }
        String[] decimalString = amount.toString().split("\\.");
        int leadingDigitsCount = decimalString[0].length();
        if (decimalString.length == 1) {
            return doGetScale(leadingDigitsCount, 0);
        }
        String trailingDigitsString = decimalString[1];

        return trailingDigitsString.charAt(trailingDigitsString.length() - 1) != '0'
                ? doGetScale(leadingDigitsCount, trailingDigitsString.length())
                : doGetScale(leadingDigitsCount, removeTrailingZeros(trailingDigitsString));
    }

    /**
     * A valid scale for the number after the decimal point is always <= AFTER_DECIMAL_MAX_DIGIT_COUNT
     * Digit count of the whole number (leading digits + trailing digits) should always be <= NUMBER_MAX_DIGIT_COUNT
     */
    private static int doGetScale(int leading, int trailing) {
        int validScale = Math.min(trailing, TRAILING_NUMBER_MAX_DIGIT_COUNT);

        return leading + validScale > WHOLE_NUMBER_MAX_DIGIT_COUNT
                ? TRAILING_NUMBER_MAX_DIGIT_COUNT
                : validScale;
    }

    /**
     * Removes trailing zeroes from given numeric string: 2350482004100000 -> 23504820041
     * get max of (length - trailing zero count, 1) to keep the decimal point
     * ex: wholeValue=25.00000, trailingDigitsString=00000 returns length=1, keeping '.0' for the whole value -> 25.0
     *
     * @return length of the number after the decimal point without trailing zeros
     */
    private static int removeTrailingZeros(final String trailingDigitsString) {
        int zeroCount = 0;
        for (int i = trailingDigitsString.length() - 1; i >= 0; i--) {
            if (trailingDigitsString.charAt(i) == '0') {
                zeroCount++;
            } else {
                return Math.max(trailingDigitsString.length() - zeroCount, 1);
            }
        }

        return trailingDigitsString.length();
    }

}
