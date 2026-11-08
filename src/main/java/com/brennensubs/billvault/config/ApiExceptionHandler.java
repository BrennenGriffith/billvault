package com.brennensubs.billvault.config;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> status(ResponseStatusException e) {
    return error(e.getStatusCode(), e.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
    return error(
        HttpStatus.BAD_REQUEST,
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining("; ")));
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ProblemDetail> malformed(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request value or JSON body");
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  public ResponseEntity<ProblemDetail> conflict(Exception e) {
    return error(
        HttpStatus.CONFLICT, "This operation conflicts with existing data. Refresh and try again.");
  }

  private ResponseEntity<ProblemDetail> error(HttpStatusCode status, String detail) {
    return ResponseEntity.status(status)
        .body(ProblemDetail.forStatusAndDetail(status, detail == null ? "Request failed" : detail));
  }
}
