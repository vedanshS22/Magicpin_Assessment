package com.magicpin.mde.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantIdentityDto {
  @NotBlank private String merchantId;
  @NotBlank private String merchantName;
  private String city;
  private String locality;
}

