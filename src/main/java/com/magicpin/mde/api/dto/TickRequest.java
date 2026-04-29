package com.magicpin.mde.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TickRequest {
  @NotBlank private String merchantId;
}

