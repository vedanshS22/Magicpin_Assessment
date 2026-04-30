package com.magicpin.mde.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChallengeTickResponse {
  List<ChallengeActionDto> actions;
}
