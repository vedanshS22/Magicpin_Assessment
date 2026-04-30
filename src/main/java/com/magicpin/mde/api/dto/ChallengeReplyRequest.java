package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Data;

@Data
public class ChallengeReplyRequest {
  @NotBlank
  @JsonProperty("conversation_id")
  private String conversationId;

  @NotBlank
  @JsonProperty("merchant_id")
  private String merchantId;

  @JsonProperty("customer_id")
  private String customerId;

  @NotBlank
  @JsonProperty("from_role")
  private String fromRole;

  @NotBlank private String message;

  @NotNull
  @JsonProperty("received_at")
  private Instant receivedAt;

  @Min(0)
  @JsonProperty("turn_number")
  private Integer turnNumber;
}
