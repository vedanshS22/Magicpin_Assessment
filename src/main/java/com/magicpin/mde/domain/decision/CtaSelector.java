package com.magicpin.mde.domain.decision;

import com.magicpin.mde.domain.model.CtaType;
import com.magicpin.mde.domain.model.Urgency;
import org.springframework.stereotype.Component;

@Component
public class CtaSelector {
  public CtaType select(Urgency urgency, int triggerScore, int merchantPain, int offerScore) {
    int heat = (int) Math.round(0.45 * triggerScore + 0.25 * offerScore + 0.30 * merchantPain);
    return switch (urgency) {
      case CRITICAL -> heat >= 70 ? CtaType.LAUNCH_NOW : CtaType.REPLY_YES;
      case HIGH -> heat >= 62 ? CtaType.REPLY_YES : CtaType.APPROVE_CAMPAIGN;
      case MEDIUM -> heat >= 55 ? CtaType.APPROVE_CAMPAIGN : CtaType.START_TODAY;
      case LOW -> heat >= 50 ? CtaType.START_TODAY : CtaType.CLAIM_OFFER;
    };
  }

  public String ctaText(CtaType cta) {
    return switch (cta) {
      case REPLY_YES -> "Reply YES";
      case LAUNCH_NOW -> "Launch Now";
      case APPROVE_CAMPAIGN -> "Approve Campaign";
      case START_TODAY -> "Start Today";
      case CLAIM_OFFER -> "Claim Offer";
    };
  }
}

