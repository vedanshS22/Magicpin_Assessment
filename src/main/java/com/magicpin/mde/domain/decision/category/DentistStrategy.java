package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class DentistStrategy implements CategoryStrategy {
  @Override
  public String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType) {
    return switch (urgency) {
      case CRITICAL, HIGH ->
          "clinical + trust + specific numbers; urgency without sounding salesy; emphasize first-visit safety/credibility";
      case MEDIUM ->
          "trust-first, calm confidence; focus on consultation/value; avoid aggressive pressure";
      case LOW ->
          "insight-led, diagnostic tone; propose a measured campaign window";
    };
  }
}

