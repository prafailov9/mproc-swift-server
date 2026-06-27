package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.service.idempotency.IdempotencyKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/keys")
public class IdempotencyKeyController {

  private final IdempotencyKeyService idempotencyKeyService;

  @Autowired
  public IdempotencyKeyController(IdempotencyKeyService idempotencyKeyService) {
    this.idempotencyKeyService = idempotencyKeyService;
  }

  @GetMapping("/all")
  public ResponseEntity<?> getAllIdempotencyKeys() {
    return ResponseEntity.ok(idempotencyKeyService.loadAll());
  }

  @DeleteMapping("/{key}")
  public ResponseEntity<?> deleteKey(@PathVariable String key) {
    idempotencyKeyService.deleteKey(key);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @DeleteMapping("/delete-all")
  public ResponseEntity<?> deleteAll() {
    idempotencyKeyService.deleteAll();
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
