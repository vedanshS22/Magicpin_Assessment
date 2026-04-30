package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChallengeHealthResponse {
  String status;

  @JsonProperty("uptime_seconds")
  long uptimeSeconds;

  @JsonProperty("contexts_loaded")
  Map<String, Long> contextsLoaded;
}
