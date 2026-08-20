package com.charity.app.config;

import com.charity.app.common.error.*;
import com.charity.app.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Translates exceptions into the single {@link ApiError} shape.
 *
 * <p>What this replaces: a handler whose catch-all mapped every {@code Exception} to 500, which
 * swallowed the {@code AuthorizationDeniedException} raised by {@code @PreAuthorize} and turned
 * every denied request into a server error; which passed {@code e.getMessage()} straight to the
 * client, leaking SQL fragments, file paths and internal class names; and which built its body with
 * {@code Map.of}, so any exception with a null message made the handler itself throw.
 *
 * <p>It has one dependency, and that is not an accident: see
 * {@link #handleIncorrectCurrentPassword}. Recording a failed password attempt is the one piece of
 * state this class writes, and it is here precisely because here is the first moment in the request
 * at which no transaction and no pooled connection is held.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LoginAttemptService loginAttempts;

    // ---------------------------------------------------------------- 401 / 403

    /**
     * One message for every credential failure. Distinguishing "no such user" from "wrong password"
     * would let anyone enumerate accounts.
     */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class, AuthenticationException.class})
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException e) {
        log.debug("Authentication failed: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("نام کاربری یا رمز عبور نادرست است", "BAD_CREDENTIALS"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> handleLocked(LockedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.RETRY_AFTER, "900")
                .body(ApiError.of(
                        "به دلیل تلاش‌های ناموفق، حساب شما موقتاً قفل شده است. لطفاً بعداً دوباره تلاش کنید.",
                        "ACCOUNT_LOCKED"));
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ResponseEntity<ApiError> handleForbidden(Exception e) {
        String message = e instanceof ForbiddenException
                ? e.getMessage()
                : "شما اجازه دسترسی به این بخش را ندارید";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(message, "ACCESS_DENIED"));
    }

    // ---------------------------------------------------------------- 404 / 410 / 409

    @ExceptionHandler({NotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(messageOr(e, "مورد درخواستی یافت نشد"), "NOT_FOUND"));
    }

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<ApiError> handleGone(GoneException e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiError.of(messageOr(e, "این مورد حذف شده است"), "GONE"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(messageOr(e, "امکان انجام این عملیات وجود ندارد"), e.getCode()));
    }

    /**
     * A constraint violation that reached the database is a bug or a race, not something the user
     * can act on, so the detail goes to the log rather than the response.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                "امکان انجام عملیات به دلیل وابستگی داده‌ها وجود ندارد", "DATA_INTEGRITY"));
    }

    // ---------------------------------------------------------------- 400 / 413

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(),
                        error.getDefaultMessage() == null ? "مقدار نامعتبر است" : error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiError.fields("اطلاعات ارسالی معتبر نیست", fields));
    }

    /** Spring 6.1 raises this instead for {@code @Validated} controller parameters. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerValidation(HandlerMethodValidationException e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("اطلاعات ارسالی معتبر نیست", "VALIDATION_FAILED"));
    }

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(messageOr(e, "درخواست نامعتبر است"), "BAD_REQUEST"));
    }

    /**
     * A wrong {@code currentPassword} on {@code PUT /api/center/me} or {@code PUT /api/admin/me}.
     * Same 400 as any other {@link ValidationException} -- this exists to count the attempt against
     * the login throttle, and to say so in the message on the one that trips the lock.
     *
     * <p><b>Counting happens here rather than at the throw site, and that is the whole point of the
     * exception.</b> The throw sites are inside the {@code @Transactional} profile-save, holding a
     * pooled connection; counting from in there asked Hikari for a second connection while the first
     * was held and deadlocked the pool once every connection was a holder. By the time an
     * {@code @ExceptionHandler} runs, the service transaction has already rolled back and returned
     * its connection -- {@code open-in-view} is false, so nothing else is holding one either -- and
     * {@link LoginAttemptService#recordFailure} simply takes a free one. That rollback is also what
     * guarantees the profile edits submitted alongside the wrong password were not saved, and it can
     * no longer take the counter down with it, because the counter is no longer inside it.
     *
     * <p>A failure to record must not turn a 400 into a 500: the caller's answer is the same either
     * way, and an exception thrown out of an exception handler lands in the container with no
     * {@link ApiError} at all. So it is caught, logged, and the un-locked message is used.
     */
    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleIncorrectCurrentPassword(IncorrectCurrentPasswordException e) {
        boolean locked = false;
        try {
            locked = loginAttempts.recordFailure(e.getUsername());
        } catch (RuntimeException failure) {
            log.error("Could not record a failed password-change attempt for '{}'", e.getUsername(), failure);
        }
        return ResponseEntity.badRequest().body(ApiError.of(
                locked ? IncorrectCurrentPasswordException.LOCKED_MESSAGE
                       : IncorrectCurrentPasswordException.MESSAGE,
                "BAD_REQUEST"));
    }

    /** Framework parse failures: the message names internal types, so it is not echoed back. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception e) {
        log.debug("Malformed request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiError.of("قالب درخواست ارسالی صحیح نیست", "MALFORMED_REQUEST"));
    }

    /**
     * Reachable now that multipart limits are configured. It previously fell through to the
     * catch-all and surfaced as a 500.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of("حجم فایل ارسالی بیش از حد مجاز است", "FILE_TOO_LARGE"));
    }

    // ---------------------------------------------------------------- 500

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        // The client gets a correlation id; the detail stays in the log where it belongs.
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled error [traceId={}]", traceId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.internal(traceId));
    }

    private static String messageOr(Exception e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }
}
