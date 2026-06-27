package com.ntros.mprocswift.controller.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.repository.transaction.IdempotencyKeyRepository;
import com.ntros.mprocswift.service.idempotency.IdempotencyKeyDataService;
import com.ntros.mprocswift.service.transfer.sync.SyncTransferService;
import com.ntros.mprocswift.service.transfer.sync.W2WTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.COMPLETED;

@Slf4j
@RestController
@RequestMapping("api/transfer/w2w")
public class W2WTransferController {

  private final SyncTransferService<W2WTransferRequest, W2WTransferResponse> transferService;

  @Autowired
  public W2WTransferController(W2WTransferService transferService) {
    this.transferService = transferService;
  }

  @PostMapping
  public ResponseEntity<?> transfer(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestBody @Validated W2WTransferRequest transferRequest) {
    transferRequest.setRequestId(idempotencyKey);
    var res = transferService.transfer(transferRequest);
    log.info("{}", res);

    switch (res.getStatus()) {
      case COMPLETED -> {
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
      }
      case IN_PROGRESS -> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("Request still processing. Retry shortly.");
      }
      case FAILED -> {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body("Original request failed.");
      }
    }
    return ResponseEntity.ofNullable(res);
  }
}
