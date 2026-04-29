package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;

public interface CategoryStrategy {
  String tone(ContextUpsertRequest ctx, Urgency urgency, TriggerType triggerType);
}

