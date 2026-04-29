package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class OtherStrategy implements CategoryStrategy {
  @Override
  public String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType) {
    return switch (urgency) {
      case CRITICAL, HIGH -> "specific + urgent + action-oriented; use numbers and time window";
      case MEDIUM -> "specific + value-led; moderate urgency; clear CTA";
      case LOW -> "insight-led; propose a controlled test rather than a full blast";
    };
  }
}

