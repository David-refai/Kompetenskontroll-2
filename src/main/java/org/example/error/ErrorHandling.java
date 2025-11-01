package org.example.error;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application-level runtime exception with optional HTTP status, error code,
 * and structured details. Framework-agnostic (no Spring).
 */
@Getter
public class ErrorHandling extends RuntimeException {


    /** Optional symbolic error code (e.g., "VALIDATION_ERROR", "NOT_FOUND"). */
    private final String code;

    /** Optional HTTP status (e.g., 400, 404, 409, 500). Use 0 if not applicable. */
    private final int status;

    /** Optional structured detail map (kept insertion-ordered). */
    private final Map<String, String> details;

    // ---- Constructors ----

    public ErrorHandling(String message, int status, String code) {
        this(message, null, status, code, null);
    }

    public ErrorHandling(String message, Throwable cause, int status, String code, Map<String, String> details) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = details == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    // ---- Static factories (handy for Http handlers) ----
    public static ErrorHandling badRequest(String message) {
        return new ErrorHandling(message, 400, "BAD_REQUEST");
    }

    public static ErrorHandling notFound(String message) {
        return new ErrorHandling(message, 404, "NOT_FOUND");
    }

}
