package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class GymStrategy implements CategoryStrategy {
  @Override
  public String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType) {
    return switch (urgency) {
      case CRITICAL, HIGH ->
          "motivation + limited trial slots; confidence and immediacy; focus on action and habit start";
      case MEDIUM ->
          "transformation + consistency; use a clear low-friction trial; moderate urgency";
      case LOW ->
          "value + plan; propose a small experiment window to lift conversions";
    };
  }
}

