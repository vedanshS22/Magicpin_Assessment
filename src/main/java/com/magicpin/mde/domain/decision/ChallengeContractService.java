package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ChallengeActionDto;
import com.magicpin.mde.api.dto.ChallengeReplyRequest;
import com.magicpin.mde.api.dto.ChallengeReplyResponse;
import com.magicpin.mde.api.dto.ChallengeTickRequest;
import com.magicpin.mde.api.dto.ChallengeTickResponse;
import com.magicpin.mde.api.dto.ReplyResponse;
import com.magicpin.mde.api.dto.TickResponse;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import com.magicpin.mde.domain.reply.IntentClassifier;
import com.magicpin.mde.domain.reply.IntentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeContractService {

  private static final String SEND_AS = "vera";
  private static final String CTA_OPEN_ENDED = "open_ended";

  private final ContextStoreService contextStoreService;
  private final DecisionOrchestratorService orchestratorService;
  private final IntentClassifier intentClassifier;

  public ChallengeTickResponse tick(ChallengeTickRequest request) {
    List<ChallengeActionDto> actions = new ArrayList<>();
    String triggerId = firstTriggerId(request.getAvailableTriggers());

    for (String merchantId : contextStoreService.merchantIds()) {
      try {
        TickResponse tick = orchestratorService.tick(merchantId);
        String selectedTriggerId = triggerId != null ? triggerId : triggerIdFromStrategy(tick.getStrategy());
        actions.add(toAction(tick, selectedTriggerId));
      } catch (RuntimeException ex) {
        log.warn("skipping merchant during challenge tick merchantId={} error={}", merchantId, ex.toString());
      }
    }

    return ChallengeTickResponse.builder()
        .actions(actions)
        .build();
  }

  public ChallengeReplyResponse reply(ChallengeReplyRequest request) {
    var intent = intentClassifier.classify(request.getMessage());

    if (intent.intent() == IntentType.STOP) {
      return ChallengeReplyResponse.builder()
          .action("end")
          .rationale("Merchant asked to stop the conversation")
          .build();
    }

    if (intent.intent() == IntentType.LATER) {
      return ChallengeReplyResponse.builder()
          .action("wait")
          .waitSeconds(1800)
          .rationale("Merchant asked to continue later")
          .build();
    }

    if (!contextStoreService.hasMerchantContext(request.getMerchantId())) {
      return ChallengeReplyResponse.builder()
          .action("send")
          .body("Thanks, I have noted this. I will follow up with the right campaign details.")
          .cta(CTA_OPEN_ENDED)
          .rationale("No active merchant context is loaded, so sending a safe acknowledgement")
          .build();
    }

    try {
      ReplyResponse reply = orchestratorService.reply(request.getMerchantId(), request.getMessage());
      return ChallengeReplyResponse.builder()
          .action("send")
          .body(reply.getMessage())
          .cta(CTA_OPEN_ENDED)
          .rationale(reply.getStrategy().getTriggerReason())
          .build();
    } catch (RuntimeException ex) {
      log.warn("reply orchestration failed merchantId={} error={}", request.getMerchantId(), ex.toString());
      return ChallengeReplyResponse.builder()
          .action("send")
          .body("Thanks, I have noted this. I will follow up with the right campaign details.")
          .cta(CTA_OPEN_ENDED)
          .rationale("Fallback acknowledgement after reply processing failed")
          .build();
    }
  }

  private static ChallengeActionDto toAction(TickResponse tick, String triggerId) {
    MessageStrategy strategy = tick.getStrategy();
    String templateName = strategy.getTriggerType().name().toLowerCase(Locale.ROOT);

    return ChallengeActionDto.builder()
        .conversationId(conversationId(tick.getMerchantId(), triggerId))
        .merchantId(tick.getMerchantId())
        .customerId(null)
        .sendAs(SEND_AS)
        .triggerId(triggerId)
        .templateName(templateName)
        .templateParams(List.of())
        .body(tick.getMessage())
        .cta(CTA_OPEN_ENDED)
        .suppressionKey(tick.getMerchantId() + ":" + triggerId)
        .rationale(strategy.getTriggerReason())
        .build();
  }

  private static String firstTriggerId(List<String> availableTriggers) {
    if (availableTriggers == null || availableTriggers.isEmpty()) {
      return null;
    }
    return availableTriggers.get(0);
  }

  private static String triggerIdFromStrategy(MessageStrategy strategy) {
    return "trg_" + strategy.getTriggerType().name().toLowerCase(Locale.ROOT);
  }

  private static String conversationId(String merchantId, String triggerId) {
    return "conv_" + safeId(merchantId) + "_" + safeId(triggerId);
  }

  private static String safeId(String value) {
    return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_]", "_");
  }
}
