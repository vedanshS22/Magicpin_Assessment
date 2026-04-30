package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ChallengeTickRequest {
  @NotNull private Instant now;

  @JsonProperty("available_triggers")
  private List<String> availableTriggers = new ArrayList<>();
}
