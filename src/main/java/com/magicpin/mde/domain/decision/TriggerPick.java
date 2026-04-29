package com.magicpin.mde.domain.decision;

import com.magicpin.mde.domain.model.TriggerType;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TriggerPick {
  TriggerType type;
  int score; // 0-100
  String reason;
  Map<String, Object> evidence;
}

