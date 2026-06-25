package com.ntros.mprocswift.utils;

import com.ntros.mprocswift.dto.transfer.TransferRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

public final class TransferRequestHasher {

  private TransferRequestHasher() {
    // Utility class
  }

  public static String payloadHash(TransferRequest request) {
    String canonicalPayload =
        field(normalize(request.getSourceAccountNumber()))
            + field(normalizeAmount(request.getAmount()))
            + field(normalizeCurrency(request.getCurrencyCode()))
            + field(normalize(request.getDescription()));

    return sha256(canonicalPayload);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static String normalizeCurrency(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private static String normalizeAmount(BigDecimal value) {
    if (value == null) {
      return "";
    }

    return value.stripTrailingZeros().toPlainString();
  }

  private static String field(String value) {
    return value.length() + ":" + value;
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to calculate SHA-256 hash", e);
    }
  }
}
