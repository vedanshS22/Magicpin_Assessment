package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.model.CtaType;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import com.magicpin.mde.domain.reply.IntentResult;
import com.magicpin.mde.domain.reply.IntentType;
import com.magicpin.mde.domain.reply.ObjectionType;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReplyDecisionService {
  private final DecisionEngineService decisionEngineService;

  public MessageStrategy decideReply(ContextUpsertRequest ctx, IntentResult intent) {
    MessageStrategy base = decisionEngineService.decideProactive(ctx);
    Map<String, Object> evidence = new LinkedHashMap<>(base.getEvidence());
    evidence.put("merchant_reply_text", intent.normalizedText());

    if (intent.intent() == IntentType.STOP) {
      return base.toBuilder()
          .triggerType(TriggerType.CAMPAIGN_INACTIVITY)
          .triggerReason("Merchant requested to stop")
          .merchantProblem("Do-not-contact request")
          .urgency(Urgency.LOW)
          .cta(CtaType.CLAIM_OFFER)
          .ctaText("Acknowledged")
          .confidenceScore(90)
          .evidence(evidence)
          .build();
    }

    if (intent.intent() == IntentType.AFFIRM) {
      evidence.put("journey_step", "merchant_approved");
      return base.toBuilder()
          .triggerReason("Merchant approved - move to launch confirmation")
          .cta(CtaType.LAUNCH_NOW)
          .ctaText("Launch Now")
          .urgency(base.getUrgency() == Urgency.LOW ? Urgency.MEDIUM : base.getUrgency())
          .confidenceScore(Math.min(100, base.getConfidenceScore() + 10))
          .evidence(evidence)
          .build();
    }

    if (intent.intent() == IntentType.MORE_INFO) {
      evidence.put("journey_step", "needs_more_info");
      return base.toBuilder()
          .triggerReason("Merchant asked for details - explain what we will send and why now")
          .cta(CtaType.REPLY_YES)
          .ctaText("Reply YES")
          .urgency(base.getUrgency() == Urgency.CRITICAL ? Urgency.HIGH : base.getUrgency())
          .confidenceScore(Math.min(100, base.getConfidenceScore() + 4))
          .evidence(evidence)
          .build();
    }

    if (intent.intent() == IntentType.LATER) {
      evidence.put("journey_step", "defer_timing");
      return base.toBuilder()
          .triggerReason("Merchant wants later - propose the next best window")
          .cta(CtaType.START_TODAY)
          .ctaText("Start Today")
          .urgency(Urgency.MEDIUM)
          .confidenceScore(Math.max(40, base.getConfidenceScore() - 6))
          .evidence(evidence)
          .build();
    }

    if (intent.intent() == IntentType.DECLINE) {
      evidence.put("journey_step", "objection_handle");
      ObjectionType obj = intent.objection();
      if (obj == ObjectionType.PRICE) {
        return base.toBuilder()
            .triggerReason("Price objection - shift to low-risk test")
            .merchantProblem("Merchant is concerned about discount/cost; reduce risk with a smaller test")
            .cta(CtaType.APPROVE_CAMPAIGN)
            .ctaText("Approve Campaign")
            .urgency(Urgency.MEDIUM)
            .confidenceScore(Math.max(35, base.getConfidenceScore() - 8))
            .evidence(evidence)
            .build();
      }
      if (obj == ObjectionType.SATURATION) {
        return base.toBuilder()
            .triggerReason("Saturation objection - run a tight segment test")
            .merchantProblem("Merchant fears over-messaging; propose a small high-intent segment burst")
            .cta(CtaType.APPROVE_CAMPAIGN)
            .ctaText("Approve Campaign")
            .urgency(Urgency.MEDIUM)
            .confidenceScore(Math.max(35, base.getConfidenceScore() - 6))
            .evidence(evidence)
            .build();
      }
      if (obj == ObjectionType.TIMING) {
        return base.toBuilder()
            .triggerReason("Timing objection - propose a better send window")
            .cta(CtaType.START_TODAY)
            .ctaText("Start Today")
            .urgency(Urgency.MEDIUM)
            .confidenceScore(Math.max(35, base.getConfidenceScore() - 6))
            .evidence(evidence)
            .build();
      }
      return base.toBuilder()
          .triggerReason("Merchant declined - offer a safer alternative")
          .cta(CtaType.START_TODAY)
          .ctaText("Start Today")
          .urgency(Urgency.LOW)
          .confidenceScore(Math.max(30, base.getConfidenceScore() - 10))
          .evidence(evidence)
          .build();
    }

    return base.toBuilder()
        .triggerReason("Unclear reply - ask a single-choice confirmation")
        .cta(CtaType.REPLY_YES)
        .ctaText("Reply YES")
        .urgency(base.getUrgency())
        .confidenceScore(base.getConfidenceScore())
        .evidence(evidence)
        .build();
  }
}

