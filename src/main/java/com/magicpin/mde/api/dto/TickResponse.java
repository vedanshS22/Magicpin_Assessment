package com.magicpin.mde.api.dto;

import com.magicpin.mde.domain.decision.MessageStrategy;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TickResponse {
  String merchantId;
  long contextVersion;
  MessageStrategy strategy;
  String message;
}

