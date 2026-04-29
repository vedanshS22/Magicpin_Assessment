package com.magicpin.mde.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantPerformanceDto {
  @NotNull @Min(0) @Max(100) private Integer conversionRatePct;
  private Integer conversionChangePct7d; // negative = dropped

  @Min(0) private Integer bookingsLast7d;
  @Min(0) @Max(100) private Integer repeatRatePct;

  @Min(0) @Max(100) private Integer campaignResponseRatePct;
  @Min(0) private Integer lastCampaignDaysAgo;
}

