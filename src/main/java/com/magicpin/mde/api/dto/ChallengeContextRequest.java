package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Data;

@Data
public class ChallengeContextRequest {
  @NotBlank private String scope;

  @NotBlank
  @JsonProperty("context_id")
  private String contextId;

  @NotNull @Min(0) private Long version;

  @NotNull private JsonNode payload;

  @NotNull
  @JsonProperty("delivered_at")
  private Instant deliveredAt;
}
