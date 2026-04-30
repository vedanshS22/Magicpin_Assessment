package com.magicpin.mde.domain.persistence;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MerchantContextEntity {
  private String merchantId;
  private long version;
  private String payload;
  private Instant updatedAt;
}

