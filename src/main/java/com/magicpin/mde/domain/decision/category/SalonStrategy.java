package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class SalonStrategy implements CategoryStrategy {
  @Override
  public String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType) {
    return switch (urgency) {
      case CRITICAL, HIGH ->
          "transformation + limited slots; crisp and energetic; use local demand numbers; push instant action";
      case MEDIUM ->
          "beauty benefit-led; gentle urgency; highlight trial/first-visit value";
      case LOW ->
          "brand-building + value; focus on improving conversion with a sharper hook";
    };
  }
}

