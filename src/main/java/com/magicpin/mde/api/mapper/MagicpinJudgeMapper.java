package com.magicpin.mde.api.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.magicpin.mde.api.dto.ChallengeActionDto;
import com.magicpin.mde.api.dto.ChallengeContextRequest;
import com.magicpin.mde.api.dto.ChallengeContextResponse;
import com.magicpin.mde.api.dto.ChallengeReplyResponse;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.MerchantIdentityDto;
import com.magicpin.mde.api.dto.MerchantPerformanceDto;
import com.magicpin.mde.api.dto.OfferDto;
import com.magicpin.mde.api.dto.ReplyResponse;
import com.magicpin.mde.api.dto.TickResponse;
import com.magicpin.mde.api.dto.TriggerContextDto;
import com.magicpin.mde.domain.decision.MessageStrategy;
import com.magicpin.mde.domain.model.Category;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MagicpinJudgeMapper {
  private static final String SEND_AS = "vera";
  private static final String CTA_OPEN_ENDED = "open_ended";

  public boolean mapsToMerchantContext(ChallengeContextRequest request) {
    return "merchant".equalsIgnoreCase(request.getScope());
  }

  public ContextUpsertRequest toContextUpsertRequest(ChallengeContextRequest request) {
    JsonNode payload = request.getPayload();
    JsonNode merchantNode = child(payload, "merchant");
    JsonNode performanceNode = child(payload, "performance");
    JsonNode triggerNode = firstChild(payload, "triggerContext", "trigger_context");

    ContextUpsertRequest ctx = new ContextUpsertRequest();
    ctx.setVersion(request.getVersion());
    ctx.setCategory(parseCategory(firstText(payload, "OTHER", "category", "merchant_category")));

    MerchantIdentityDto merchant = new MerchantIdentityDto();
    merchant.setMerchantId(firstText(merchantNode, request.getContextId(), "merchantId", "merchant_id", "id"));
    merchant.setMerchantName(firstText(merchantNode, merchant.getMerchantId(), "merchantName", "merchant_name", "name"));
    merchant.setCity(firstText(merchantNode, null, "city"));
    merchant.setLocality(firstText(merchantNode, null, "locality", "area"));
    ctx.setMerchant(merchant);

    MerchantPerformanceDto performance = new MerchantPerformanceDto();
    performance.setConversionRatePct(firstInt(performanceNode, 20, "conversionRatePct", "conversion_rate_pct", "conversion"));
    performance.setConversionChangePct7d(firstInt(performanceNode, null, "conversionChangePct7d", "conversion_change_pct_7d"));
    performance.setBookingsLast7d(firstInt(performanceNode, 0, "bookingsLast7d", "bookings_last_7d", "bookings"));
    performance.setRepeatRatePct(firstInt(performanceNode, 0, "repeatRatePct", "repeat_rate_pct"));
    performance.setCampaignResponseRatePct(firstInt(performanceNode, 0, "campaignResponseRatePct", "campaign_response_rate_pct"));
    performance.setLastCampaignDaysAgo(firstInt(performanceNode, null, "lastCampaignDaysAgo", "last_campaign_days_ago"));
    ctx.setPerformance(performance);

    TriggerContextDto trigger = new TriggerContextDto();
    trigger.setNearbySearchesCountToday(firstInt(triggerNode, null, "nearbySearchesCountToday", "nearby_searches_count_today"));
    trigger.setTopSearchKeyword(firstText(triggerNode, null, "topSearchKeyword", "top_search_keyword", "keyword"));
    trigger.setSearchChangePct3d(firstInt(triggerNode, null, "searchChangePct3d", "search_change_pct_3d"));
    trigger.setLocalFootfallIndex(firstInt(triggerNode, null, "localFootfallIndex", "local_footfall_index"));
    trigger.setWeekendFlag(firstBoolean(triggerNode, "weekendFlag", "weekend_flag"));
    trigger.setCompetitorActivityFlag(firstBoolean(triggerNode, "competitorActivityFlag", "competitor_activity_flag"));
    trigger.setCustomerResponseProbabilityPct(firstInt(triggerNode, null, "customerResponseProbabilityPct", "customer_response_probability_pct"));
    trigger.setAbandonedLeadsLast48h(firstInt(triggerNode, null, "abandonedLeadsLast48h", "abandoned_leads_last_48h"));
    ctx.setTriggerContext(trigger);

    JsonNode offers = child(payload, "offers");
    if (offers != null && offers.isArray()) {
      int index = 0;
      for (JsonNode offerNode : offers) {
        ctx.getOffers().add(toOffer(offerNode, index++));
      }
    }

    return ctx;
  }

  public ChallengeContextResponse acceptedContext(Instant storedAt) {
    return ChallengeContextResponse.builder()
        .accepted(true)
        .ackId("ack_" + UUID.randomUUID().toString().replace("-", ""))
        .storedAt(storedAt)
        .build();
  }

  public ChallengeContextResponse staleContext(long currentVersion) {
    return ChallengeContextResponse.builder()
        .accepted(false)
        .reason("stale_version")
        .currentVersion(currentVersion)
        .build();
  }

  public ChallengeActionDto toAction(TickResponse tick, String triggerId) {
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

  public ChallengeReplyResponse sendReply(ReplyResponse reply) {
    return ChallengeReplyResponse.builder()
        .action("send")
        .body(reply.getMessage())
        .cta(CTA_OPEN_ENDED)
        .rationale(reply.getStrategy().getTriggerReason())
        .build();
  }

  public ChallengeReplyResponse fallbackSend(String rationale) {
    return ChallengeReplyResponse.builder()
        .action("send")
        .body("Thanks, I have noted this. I will follow up with the right campaign details.")
        .cta(CTA_OPEN_ENDED)
        .rationale(rationale)
        .build();
  }

  public ChallengeReplyResponse waitReply(String rationale) {
    return ChallengeReplyResponse.builder()
        .action("wait")
        .waitSeconds(1800)
        .rationale(rationale)
        .build();
  }

  public ChallengeReplyResponse endReply(String rationale) {
    return ChallengeReplyResponse.builder()
        .action("end")
        .rationale(rationale)
        .build();
  }

  public String firstTriggerId(List<String> availableTriggers) {
    if (availableTriggers == null || availableTriggers.isEmpty()) {
      return null;
    }
    return availableTriggers.get(0);
  }

  public String triggerIdFromStrategy(MessageStrategy strategy) {
    return "trg_" + strategy.getTriggerType().name().toLowerCase(Locale.ROOT);
  }

  private static String conversationId(String merchantId, String triggerId) {
    return "conv_" + safeId(merchantId) + "_" + safeId(triggerId);
  }

  private static String safeId(String value) {
    return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_]", "_");
  }

  private static OfferDto toOffer(JsonNode node, int index) {
    OfferDto offer = new OfferDto();
    offer.setOfferId(firstText(node, "offer_" + index, "offerId", "offer_id", "id"));
    offer.setTitle(firstText(node, "First-visit offer", "title", "name"));
    offer.setType(parseOfferType(firstText(node, "FIRST_VISIT", "type", "offer_type")));
    offer.setPriceInr(firstInt(node, null, "priceInr", "price_inr", "price"));
    offer.setOriginalPriceInr(firstInt(node, null, "originalPriceInr", "original_price_inr"));
    offer.setDiscountPercent(firstInt(node, null, "discountPercent", "discount_percent"));
    offer.setConstraints(firstText(node, null, "constraints"));
    return offer;
  }

  private static Category parseCategory(String value) {
    if (value == null || value.isBlank()) {
      return Category.OTHER;
    }
    try {
      return Category.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    } catch (IllegalArgumentException ignored) {
      return Category.OTHER;
    }
  }

  private static OfferDto.OfferType parseOfferType(String value) {
    if (value == null || value.isBlank()) {
      return OfferDto.OfferType.FIRST_VISIT;
    }
    try {
      return OfferDto.OfferType.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    } catch (IllegalArgumentException ignored) {
      return OfferDto.OfferType.FIRST_VISIT;
    }
  }

  private static JsonNode child(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.get(field).isNull()) {
      return node;
    }
    return node.get(field);
  }

  private static JsonNode firstChild(JsonNode node, String... fields) {
    if (node == null) {
      return null;
    }
    for (String field : fields) {
      if (node.has(field) && !node.get(field).isNull()) {
        return node.get(field);
      }
    }
    return node;
  }

  private static String firstText(JsonNode node, String defaultValue, String... fields) {
    if (node != null) {
      for (String field : fields) {
        if (node.has(field) && !node.get(field).isNull()) {
          return node.get(field).asText();
        }
      }
    }
    return defaultValue;
  }

  private static Integer firstInt(JsonNode node, Integer defaultValue, String... fields) {
    if (node != null) {
      for (String field : fields) {
        if (node.has(field) && !node.get(field).isNull()) {
          return node.get(field).asInt();
        }
      }
    }
    return defaultValue;
  }

  private static Boolean firstBoolean(JsonNode node, String... fields) {
    if (node != null) {
      for (String field : fields) {
        if (node.has(field) && !node.get(field).isNull()) {
          return node.get(field).asBoolean();
        }
      }
    }
    return null;
  }
}
