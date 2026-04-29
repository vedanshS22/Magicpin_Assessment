package com.magicpin.mde.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContextUpsertResponse {
  String merchantId;
  long version;
  boolean stored;
  String reason;
}

