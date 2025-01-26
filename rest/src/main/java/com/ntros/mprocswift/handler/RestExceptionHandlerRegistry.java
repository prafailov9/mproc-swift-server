package com.ntros.mprocswift.handler;

import com.ntros.mprocswift.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RestExceptionHandlerRegistry {

    private static final List<RestExceptionHandler> HANDLERS;

    // register all handlers
    static {
        HANDLERS = List.of(GenericExceptionHandler.of(WalletCreateFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(WalletCreateFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(WalletNotFoundException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(WalletNotFoundForAccountException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(WalletNotFoundForCurrencyAndAccountException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(WalletDeleteFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(AccountConstraintFailureException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(AccountNotFoundException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(CurrencyNotFoundException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(FailedToActivateAllCurrenciesException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(CurrencyInactiveStateException.class, HttpStatus.BAD_REQUEST, Throwable::getMessage),
                GenericExceptionHandler.of(AddressNotFoundException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(AddressHashingFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(AddressConstraintFailureException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(TransferProcessingFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(DataConstraintViolationException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(WalletUpdateFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(InvalidTransferRequestException.class, HttpStatus.BAD_REQUEST, Throwable::getMessage),
                GenericExceptionHandler.of(WalletNotFoundForANException.class, HttpStatus.BAD_REQUEST, Throwable::getMessage),
                GenericExceptionHandler.of(NoMainWalletException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(InsufficientFundsException.class, HttpStatus.BAD_REQUEST, Throwable::getMessage),
                GenericExceptionHandler.of(CurrencyNotSupportedException.class, HttpStatus.BAD_REQUEST, Throwable::getMessage),
                GenericExceptionHandler.of(ExchangeRateNotFoundForPairException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(InvalidDecimalAmountException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(CardNotFoundException.class, HttpStatus.NOT_FOUND, Throwable::getMessage),
                GenericExceptionHandler.of(CannotRefreshCardException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(CardNotCreatedException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(MerchantConstraintFailureException.class, HttpStatus.INTERNAL_SERVER_ERROR, Throwable::getMessage),
                GenericExceptionHandler.of(NotFoundException.class, HttpStatus.NOT_FOUND, Throwable::getMessage)
                );

    }

    private RestExceptionHandlerRegistry() {

    }

    public static ResponseEntity<?> handleException(Throwable ex) {
        Optional<RestExceptionHandler> restExceptionHandler = HANDLERS.stream()
                .filter(handler -> handler.supports(ex.getClass()))
                .findFirst();
        return restExceptionHandler.isPresent() ? restExceptionHandler.get().handle(ex) : defaultErrorResponse(ex);
    }

    private static ResponseEntity<?> defaultErrorResponse(Throwable ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }

}
