package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChallengeContextResponse {
  boolean accepted;

  @JsonProperty("ack_id")
  String ackId;

  @JsonProperty("stored_at")
  Instant storedAt;

  String reason;

  @JsonProperty("current_version")
  Long currentVersion;
}
