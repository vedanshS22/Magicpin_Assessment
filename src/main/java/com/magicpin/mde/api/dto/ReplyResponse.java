package com.magicpin.mde.api.dto;

import com.magicpin.mde.domain.decision.MessageStrategy;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReplyResponse {
  String merchantId;
  long contextVersion;
  String intent;
  String objection;
  MessageStrategy strategy;
  String message;
}

