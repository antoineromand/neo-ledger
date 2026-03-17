package org.neo_ledger_transaction.web;

import org.neo_ledger.common.ApplicationException;
import org.neo_ledger.common.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.xml.parsers.ParserConfigurationException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiErrorResponse(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST.value(),
                                LocalDateTime.now()
                        )
                );
    }

    @ExceptionHandler(ParserConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> handleInternalConfig(ParserConfigurationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiErrorResponse(
                                "Intern Error Server",
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                LocalDateTime.now())
                );
    }
}
