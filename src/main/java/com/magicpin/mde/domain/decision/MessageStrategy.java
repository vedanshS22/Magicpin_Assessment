package com.magicpin.mde.domain.decision;

import com.magicpin.mde.domain.model.Category;
import com.magicpin.mde.domain.model.CtaType;
import com.magicpin.mde.domain.model.TriggerType;
import com.magicpin.mde.domain.model.Urgency;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MessageStrategy {
  TriggerType triggerType;
  String triggerReason;

  String merchantProblem;

  String offerSelected;
  String offerId;

  Urgency urgency;
  Category category;
  String categoryTone;

  CtaType cta;
  String ctaText;

  int confidenceScore; // 0-100

  Map<String, Object> evidence; // numbers used in message
}

