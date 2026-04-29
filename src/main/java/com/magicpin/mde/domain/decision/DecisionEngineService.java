package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.decision.category.CategoryStrategy;
import com.magicpin.mde.domain.decision.category.CategoryStrategyFactory;
import com.magicpin.mde.domain.model.CtaType;
import com.magicpin.mde.domain.model.Urgency;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DecisionEngineService {
  private final TriggerScorer triggerScorer;
  private final MerchantHealthScorer merchantHealthScorer;
  private final OfferStrengthScorer offerStrengthScorer;
  private final UrgencyDecider urgencyDecider;
  private final CtaSelector ctaSelector;
  private final CategoryStrategyFactory categoryStrategyFactory;

  public MessageStrategy decideProactive(ContextUpsertRequest ctx) {
    var trigger = triggerScorer.pickBest(ctx);
    int merchantPain = merchantHealthScorer.score(ctx);
    var offer = offerStrengthScorer.pickBest(ctx);

    CategoryStrategy categoryStrategy = categoryStrategyFactory.forCategory(ctx.getCategory());
    Urgency urgency = urgencyDecider.decide(ctx, trigger.getScore(), merchantPain, offer.getScore());
    CtaType cta = ctaSelector.select(urgency, trigger.getScore(), merchantPain, offer.getScore());

    int confidence =
        clamp0to100(
            (int)
                Math.round(
                    0.40 * trigger.getScore()
                        + 0.25 * offer.getScore()
                        + 0.20 * merchantPain
                        + 0.15 * safePct(ctx)));

    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.putAll(trigger.getEvidence());
    evidence.putAll(offer.getEvidence());
    evidence.put("merchant_conversion_rate_pct", ctx.getPerformance().getConversionRatePct());
    evidence.put("merchant_conversion_change_pct_7d", ctx.getPerformance().getConversionChangePct7d());
    evidence.put("customer_response_probability_pct", safePct(ctx));

    var merchantProblem = merchantHealthScorer.problemStatement(ctx);
    var triggerReason = trigger.getReason();
    var offerSelected = offer.getOfferSelected();
    var tone = categoryStrategy.tone(ctx, urgency, trigger.getType());
    var ctaText = ctaSelector.ctaText(cta);

    return MessageStrategy.builder()
        .triggerType(trigger.getType())
        .triggerReason(triggerReason)
        .merchantProblem(merchantProblem)
        .offerSelected(offerSelected)
        .offerId(offer.getOfferId())
        .urgency(urgency)
        .category(ctx.getCategory())
        .categoryTone(tone)
        .cta(cta)
        .ctaText(ctaText)
        .confidenceScore(confidence)
        .evidence(evidence)
        .build();
  }

  private static int safePct(ContextUpsertRequest ctx) {
    if (ctx.getTriggerContext() == null || ctx.getTriggerContext().getCustomerResponseProbabilityPct() == null) {
      return 55; // deterministic fallback; mildly optimistic but not extreme
    }
    return ctx.getTriggerContext().getCustomerResponseProbabilityPct();
  }

  private static int clamp0to100(int x) {
    return Math.max(0, Math.min(100, x));
  }
}

