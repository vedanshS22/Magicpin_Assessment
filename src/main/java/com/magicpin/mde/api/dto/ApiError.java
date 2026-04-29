package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
  String code;
  String message;
  Map<String, Object> details;
}

