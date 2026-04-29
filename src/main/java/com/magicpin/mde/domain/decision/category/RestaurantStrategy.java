package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class RestaurantStrategy implements CategoryStrategy {
  @Override
  public String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType) {
    return switch (urgency) {
      case CRITICAL, HIGH ->
          "timing + hunger + now/tonight framing; fast, punchy; mention footfall/searches; tight CTA";
      case MEDIUM ->
          "value-led with time window; avoid overpressure; mention peak hours/weekend";
      case LOW ->
          "insight-led; suggest a controlled burst campaign (2-3 hours) to test";
    };
  }
}

