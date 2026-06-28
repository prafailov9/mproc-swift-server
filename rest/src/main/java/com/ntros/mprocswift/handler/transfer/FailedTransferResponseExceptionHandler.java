package com.ntros.mprocswift.handler.transfer;

import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.exceptions.IdempotencyKeyConflictException;
import com.ntros.mprocswift.exceptions.TransferProcessingFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.FAILED;

@ControllerAdvice
public class FailedTransferResponseExceptionHandler {

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(TransferProcessingFailedException.class)
  public W2WTransferResponse handleFailedResponse(TransferProcessingFailedException exception) {
    // TODO: make generic for all transfer types

    W2WTransferResponse response = new W2WTransferResponse();
    response.setStatus(FAILED);
    response.setDescription("Error: " + exception.getMessage());
    response.setFresh(true);
    return response;
  }

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(IdempotencyKeyConflictException.class)
  public W2WTransferResponse handleFailedIdempotencyCheck(
      IdempotencyKeyConflictException exception) {

    W2WTransferResponse response = new W2WTransferResponse();
    response.setStatus(FAILED);
    response.setDescription("Error: " + exception.getMessage());
    response.setFresh(true);
    return response;
  }
}
