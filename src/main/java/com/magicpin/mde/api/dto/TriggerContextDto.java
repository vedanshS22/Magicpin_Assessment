package com.magicpin.mde.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TriggerContextDto {
  @Min(0) private Integer nearbySearchesCountToday;
  private String topSearchKeyword;
  private Integer searchChangePct3d;

  @Min(0) private Integer localFootfallIndex; // 0-200, relative
  private Boolean weekendFlag;
  private Boolean competitorActivityFlag;

  @Min(0) @Max(100) private Integer customerResponseProbabilityPct;
  @Min(0) private Integer abandonedLeadsLast48h;
}

