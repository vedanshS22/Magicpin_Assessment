package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  String requestId;
  Instant timestamp;
  T data;
  ApiError error;

  public static <T> ApiResponse<T> ok(String requestId, T data) {
    return ApiResponse.<T>builder().requestId(requestId).timestamp(Instant.now()).data(data).build();
  }

  public static <T> ApiResponse<T> fail(String requestId, ApiError error) {
    return ApiResponse.<T>builder().requestId(requestId).timestamp(Instant.now()).error(error).build();
  }
}

