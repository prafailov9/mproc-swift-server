package com.ntros.mprocswift.controller.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyRecord;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus;
import com.ntros.mprocswift.repository.transaction.IdempotencyRecordRepository;
import com.ntros.mprocswift.service.idempotency.IdempotencyRecordDataService;
import com.ntros.mprocswift.service.transfer.synch.SyncTransferService;
import com.ntros.mprocswift.service.transfer.synch.W2WTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.COMPLETED;

@RestController
@RequestMapping("api/transfer/w2w")
public class W2WTransferController {

  private final IdempotencyRecordRepository idempotencyRecordRepository;
  private final ObjectMapper objectMapper;

  private final SyncTransferService<W2WTransferRequest, W2WTransferResponse> transferService;
  private final IdempotencyRecordDataService idempotencyRecordDataService;

  @Autowired
  public W2WTransferController(
      W2WTransferService transferService,
      IdempotencyRecordDataService idempotencyRecordDataService,
      ObjectMapper objectMapper,
      IdempotencyRecordRepository idempotencyRecordRepository) {
    this.transferService = transferService;
    this.idempotencyRecordDataService = idempotencyRecordDataService;
    this.objectMapper = objectMapper;
    this.idempotencyRecordRepository = idempotencyRecordRepository;
  }

  @PostMapping
  public ResponseEntity<?> transfer(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestBody @Validated W2WTransferRequest transferRequest)
      throws JsonProcessingException {

    String requestJson = objectMapper.writeValueAsString(transferRequest);
    String requestHash = sha256Hex(requestJson);

    Optional<IdempotencyRecord> existing =
        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

    if (existing.isPresent()) {
      var record = existing.get();
      if (!record.getRequestHash().equals(requestHash)) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("Idempotency-Key reused with different req payload.");
      }
      W2WTransferResponse replay =
          objectMapper.readValue(record.getResponseBody(), W2WTransferResponse.class);
      replay.setFresh(false);
      return ResponseEntity.status(record.getStatusCode()).body(replay);
    }

    var response = transferService.transfer(transferRequest);
    String responseBody = objectMapper.writeValueAsString(response);

    var record = new IdempotencyRecord();
    record.setIdempotencyKey(idempotencyKey);
    record.setRequestHash(requestHash);
    record.setStatus(COMPLETED);
    record.setStatusCode(HttpStatus.CREATED.value());
    record.setResponseBody(responseBody);
    idempotencyRecordDataService.saveRecord(record);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/keys")
  public ResponseEntity<?> getAllKeys() {
    return ResponseEntity.ok(idempotencyRecordDataService.loadAll());
  }

  private String sha256Hex(String input) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
