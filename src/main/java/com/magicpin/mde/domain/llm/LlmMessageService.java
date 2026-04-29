package com.magicpin.mde.domain.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.config.AppProperties;
import com.magicpin.mde.domain.decision.MessageStrategy;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmMessageService {
  private final AppProperties appProperties;
  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;
  private final DeterministicTemplateRenderer fallback = new DeterministicTemplateRenderer();

  public String render(MessageStrategy strategy, ContextUpsertRequest ctx) {
    if (!appProperties.getOpenai().isEnabled()) {
      return fallback.render(strategy, ctx);
    }

    String prompt = buildPrompt(strategy, ctx);
    try {
      String out = openAiClient.generate(prompt);
      if (out == null || out.isBlank()) return fallback.render(strategy, ctx);
      return postProcess(out, strategy);
    } catch (Exception ex) {
      log.warn("LLM render failed, falling back: {}", ex.toString());
      return fallback.render(strategy, ctx);
    }
  }

  private String buildPrompt(MessageStrategy s, ContextUpsertRequest ctx) {
    Map<String, Object> payload =
        Map.of(
            "date", LocalDate.now().toString(),
            "merchant",
                Map.of(
                    "merchantId", ctx.getMerchant().getMerchantId(),
                    "merchantName", ctx.getMerchant().getMerchantName(),
                    "city", ctx.getMerchant().getCity(),
                    "locality", ctx.getMerchant().getLocality(),
                    "category", ctx.getCategory().name()),
            "strategy",
                Map.of(
                    "trigger_type", s.getTriggerType().name(),
                    "trigger_reason", s.getTriggerReason(),
                    "merchant_problem", s.getMerchantProblem(),
                    "offer_selected", s.getOfferSelected(),
                    "urgency", s.getUrgency().name(),
                    "category_tone", s.getCategoryTone(),
                    "cta", s.getCtaText(),
                    "confidence_score", s.getConfidenceScore(),
                    "evidence", s.getEvidence()));

    String strategyJson;
    try {
      strategyJson = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      strategyJson = payload.toString();
    }

    return ""
        + "You will write ONE message to a merchant. This is NOT a chatbot.\n"
        + "The strategy is already decided. You must ONLY convert it into compelling wording.\n\n"
        + "Hard rules:\n"
        + "- Output must be deterministic: no randomness, no multiple options.\n"
        + "- Use specific numbers from evidence (at least 2 numbers) if present.\n"
        + "- Mention the offer exactly as provided.\n"
        + "- One CTA only, use the exact CTA text.\n"
        + "- 2 to 5 short lines. No bullets.\n"
        + "- No generic phrases like 'boost your business' or 'limited time offer' without a factual time window.\n\n"
        + "Strategy JSON:\n"
        + strategyJson
        + "\n\n"
        + "Write the final merchant message now.";
  }

  private static String postProcess(String out, MessageStrategy s) {
    String trimmed = out.trim();
    // Enforce single CTA line at end (best-effort)
    if (!trimmed.contains(s.getCtaText())) {
      trimmed = trimmed + "\n\n" + s.getCtaText() + ".";
    }
    return trimmed;
  }
}

