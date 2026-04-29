package com.magicpin.mde.api.exception;

import com.magicpin.mde.api.dto.ApiError;
import com.magicpin.mde.api.dto.ApiResponse;
import com.magicpin.mde.util.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    var err = ApiError.builder().code(ex.getCode()).message(ex.getMessage()).build();
    return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(requestId, err));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    Map<String, Object> details = new LinkedHashMap<>();
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fields.put(fe.getField(), fe.getDefaultMessage());
    }
    details.put("fields", fields);
    var err = ApiError.builder().code("VALIDATION_ERROR").message("Invalid request").details(details).build();
    return ResponseEntity.badRequest().body(ApiResponse.fail(requestId, err));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    var err = ApiError.builder().code("VALIDATION_ERROR").message(ex.getMessage()).build();
    return ResponseEntity.badRequest().body(ApiResponse.fail(requestId, err));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    log.error("unhandled exception", ex);
    var err =
        ApiError.builder()
            .code("INTERNAL_ERROR")
            .message("Unexpected error")
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(requestId, err));
  }
}

