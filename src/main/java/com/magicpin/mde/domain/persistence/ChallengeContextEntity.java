package com.magicpin.mde.domain.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengeContextEntity {
  private String scope;
  private String contextId;
  private long version;
  private JsonNode payload;
  private Instant deliveredAt;
  private Instant storedAt;
}
