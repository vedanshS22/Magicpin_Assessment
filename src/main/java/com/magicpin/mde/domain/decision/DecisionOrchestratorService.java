package com.magicpin.mde.domain.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.ReplyResponse;
import com.magicpin.mde.api.dto.TickResponse;
import com.magicpin.mde.domain.llm.LlmMessageService;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import com.magicpin.mde.domain.reply.IntentClassifier;
import com.magicpin.mde.domain.reply.IntentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecisionOrchestratorService {

  private final ContextStoreService contextStoreService;
  private final ObjectMapper objectMapper;
  private final DecisionEngineService decisionEngineService;
  private final IntentClassifier intentClassifier;
  private final ReplyDecisionService replyDecisionService;
  private final LlmMessageService llmMessageService;

  public TickResponse tick(String merchantId) {
    var ctxEntity = contextStoreService.getRequired(merchantId);

    ContextUpsertRequest ctx;

    try {
      ctx = objectMapper.readValue(
              ctxEntity.getPayload(),
              ContextUpsertRequest.class
      );
    } catch (Exception e) {
      throw new RuntimeException(
              "Failed to deserialize merchant context payload",
              e
      );
    }

    MessageStrategy strategy =
            decisionEngineService.decideProactive(ctx);

    String msg =
            llmMessageService.render(strategy, ctx);

    return TickResponse.builder()
            .merchantId(merchantId)
            .contextVersion(ctxEntity.getVersion())
            .strategy(strategy)
            .message(msg)
            .build();
  }

  public ReplyResponse reply(String merchantId, String text) {
    var ctxEntity = contextStoreService.getRequired(merchantId);

    ContextUpsertRequest ctx;

    try {
      ctx = objectMapper.readValue(
              ctxEntity.getPayload(),
              ContextUpsertRequest.class
      );
    } catch (Exception e) {
      throw new RuntimeException(
              "Failed to deserialize merchant context payload",
              e
      );
    }

    IntentResult intent =
            intentClassifier.classify(text);

    MessageStrategy strategy =
            replyDecisionService.decideReply(ctx, intent);

    String msg =
            llmMessageService.render(strategy, ctx);

    return ReplyResponse.builder()
            .merchantId(merchantId)
            .contextVersion(ctxEntity.getVersion())
            .intent(intent.intent().name())
            .objection(
                    intent.objection() == null
                            ? null
                            : intent.objection().name()
            )
            .strategy(strategy)
            .message(msg)
            .build();
  }
}