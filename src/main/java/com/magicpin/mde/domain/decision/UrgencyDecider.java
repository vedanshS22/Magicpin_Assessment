package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.TriggerContextDto;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class UrgencyDecider {
  public Urgency decide(ContextUpsertRequest ctx, int triggerScore, int merchantPain, int offerScore) {
    TriggerContextDto t = ctx.getTriggerContext();
    int demand = t == null || t.getNearbySearchesCountToday() == null ? 0 : t.getNearbySearchesCountToday();
    boolean weekend = t != null && Boolean.TRUE.equals(t.getWeekendFlag());
    int responseProb = t == null || t.getCustomerResponseProbabilityPct() == null ? 55 : t.getCustomerResponseProbabilityPct();

    int composite =
        (int)
            Math.round(
                0.35 * triggerScore
                    + 0.25 * offerScore
                    + 0.20 * merchantPain
                    + 0.20 * responseProb);

    if (weekend) composite += 6;
    if (demand >= 150) composite += 8;

    if (composite >= 80) return Urgency.CRITICAL;
    if (composite >= 66) return Urgency.HIGH;
    if (composite >= 50) return Urgency.MEDIUM;
    return Urgency.LOW;
  }
}

