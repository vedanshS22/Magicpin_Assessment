package com.magicpin.mde.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyRequest {
  @NotBlank private String merchantId;
  @NotBlank private String text;
}

