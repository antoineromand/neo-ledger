package org.neo_ledger.common.response;

import java.time.LocalDateTime;

public record ApiErrorResponse(String message, int status, LocalDateTime timestamp) {
}
