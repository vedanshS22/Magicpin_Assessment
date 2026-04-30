package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ChallengeActionDto;
import com.magicpin.mde.api.dto.ChallengeReplyRequest;
import com.magicpin.mde.api.dto.ChallengeReplyResponse;
import com.magicpin.mde.api.dto.ChallengeTickRequest;
import com.magicpin.mde.api.dto.ChallengeTickResponse;
import com.magicpin.mde.api.dto.TickResponse;
import com.magicpin.mde.api.mapper.MagicpinJudgeMapper;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import com.magicpin.mde.domain.reply.IntentClassifier;
import com.magicpin.mde.domain.reply.IntentType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeContractService {

  private final ContextStoreService contextStoreService;
  private final DecisionOrchestratorService orchestratorService;
  private final IntentClassifier intentClassifier;
  private final MagicpinJudgeMapper mapper;

  public ChallengeTickResponse tick(ChallengeTickRequest request) {
    List<ChallengeActionDto> actions = new ArrayList<>();
    String requestedTriggerId = mapper.firstTriggerId(request.getAvailableTriggers());

    for (String merchantId : contextStoreService.merchantIds()) {
      try {
        TickResponse tick = orchestratorService.tick(merchantId);
        String triggerId =
            requestedTriggerId != null ? requestedTriggerId : mapper.triggerIdFromStrategy(tick.getStrategy());
        actions.add(mapper.toAction(tick, triggerId));
      } catch (RuntimeException ex) {
        log.warn("skipping merchant during judge tick merchantId={} error={}", merchantId, ex.toString());
      }
    }

    return ChallengeTickResponse.builder()
        .actions(actions)
        .build();
  }

  public ChallengeReplyResponse reply(ChallengeReplyRequest request) {
    var intent = intentClassifier.classify(request.getMessage());

    if (intent.intent() == IntentType.STOP) {
      return mapper.endReply("Merchant asked to stop the conversation");
    }

    if (intent.intent() == IntentType.LATER) {
      return mapper.waitReply("Merchant asked to continue later");
    }

    if (!contextStoreService.hasMerchantContext(request.getMerchantId())) {
      return mapper.fallbackSend("No active merchant context is loaded, so sending a safe acknowledgement");
    }

    try {
      return mapper.sendReply(orchestratorService.reply(request.getMerchantId(), request.getMessage()));
    } catch (RuntimeException ex) {
      log.warn("reply orchestration failed merchantId={} error={}", request.getMerchantId(), ex.toString());
      return mapper.fallbackSend("Fallback acknowledgement after reply processing failed");
    }
  }
}
