package com.adaptiveplanner.planner.web;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = PlannerWorkspaceRestController.class)
public class PlannerApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> statusError(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        return ResponseEntity.status(status).body(new ApiError(exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "The planner request is invalid." : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    public record ApiError(String error) { }
}
