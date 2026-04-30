package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChallengeActionDto {
  @JsonProperty("conversation_id")
  String conversationId;

  @JsonProperty("merchant_id")
  String merchantId;

  @JsonProperty("customer_id")
  String customerId;

  @JsonProperty("send_as")
  String sendAs;

  @JsonProperty("trigger_id")
  String triggerId;

  @JsonProperty("template_name")
  String templateName;

  @JsonProperty("template_params")
  List<Object> templateParams;

  String body;
  String cta;

  @JsonProperty("suppression_key")
  String suppressionKey;

  String rationale;
}
