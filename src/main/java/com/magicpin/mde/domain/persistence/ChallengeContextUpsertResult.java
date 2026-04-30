package com.magicpin.mde.domain.persistence;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChallengeContextUpsertResult {
  boolean accepted;
  long currentVersion;
  Instant storedAt;
}
