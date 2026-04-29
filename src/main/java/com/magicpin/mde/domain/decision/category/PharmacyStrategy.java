package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class PharmacyStrategy implements CategoryStrategy {
  @Override
  public String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType) {
    return switch (urgency) {
      case CRITICAL, HIGH ->
          "utility + reliability; urgency must be factual (availability/time window); avoid hype; clear action";
      case MEDIUM ->
          "service-led; convenience framing; steady urgency";
      case LOW ->
          "trust + repeat; focus on reactivation and retention";
    };
  }
}

