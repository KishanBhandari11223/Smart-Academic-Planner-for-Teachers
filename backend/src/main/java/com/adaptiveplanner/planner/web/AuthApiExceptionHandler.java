package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.InvalidOtpRequestException;
import com.adaptiveplanner.planner.auth.OtpDeliveryUnavailableException;
import com.adaptiveplanner.planner.auth.OtpRateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AuthRestController.class)
public class AuthApiExceptionHandler {
    @ExceptionHandler(InvalidOtpRequestException.class)
    public ResponseEntity<ApiError> invalidOtp(InvalidOtpRequestException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(OtpRateLimitException.class)
    public ResponseEntity<ApiError> rateLimited(OtpRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(OtpDeliveryUnavailableException.class)
    public ResponseEntity<ApiError> deliveryUnavailable(OtpDeliveryUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "The request is invalid." : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    public record ApiError(String error) { }
}
