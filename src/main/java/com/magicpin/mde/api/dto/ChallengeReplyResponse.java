package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChallengeReplyResponse {
  String action;
  String body;
  String cta;

  @JsonProperty("wait_seconds")
  Integer waitSeconds;

  String rationale;
}
