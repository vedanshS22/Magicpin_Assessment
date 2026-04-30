package com.magicpin.mde.domain.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.magicpin.mde.api.dto.ChallengeContextRequest;
import com.magicpin.mde.api.dto.ChallengeContextResponse;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.ContextUpsertResponse;
import com.magicpin.mde.api.dto.MerchantIdentityDto;
import com.magicpin.mde.api.dto.MerchantPerformanceDto;
import com.magicpin.mde.api.dto.OfferDto;
import com.magicpin.mde.api.dto.TriggerContextDto;
import com.magicpin.mde.api.exception.ApiException;
import com.magicpin.mde.domain.model.Category;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextStoreService {

  private final ObjectMapper objectMapper;
  private final ConcurrentMap<String, MerchantContextEntity> contexts = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, ChallengeContextEntity> challengeContexts = new ConcurrentHashMap<>();

  public ContextUpsertResponse upsert(ContextUpsertRequest request) {
    String merchantId = request.getMerchant().getMerchantId();
    long incomingVersion = request.getVersion();

    if (merchantId == null || merchantId.isBlank()) {
      throw ApiException.badRequest(
          "INVALID_MERCHANT_ID",
          "merchant.merchantId is required");
    }

    AtomicBoolean stored = new AtomicBoolean(false);
    MerchantContextEntity entity =
        contexts.compute(
            merchantId,
            (key, existing) -> {
              if (existing != null && existing.getVersion() >= incomingVersion) {
                return existing;
              }

              MerchantContextEntity next = new MerchantContextEntity();
              next.setMerchantId(merchantId);
              next.setVersion(incomingVersion);
              next.setPayload(toPayload(request));
              next.setUpdatedAt(Instant.now());
              stored.set(true);
              return next;
            });

    if (!stored.get()) {
      return ContextUpsertResponse.builder()
          .merchantId(merchantId)
          .version(entity.getVersion())
          .stored(false)
          .reason("ignored_same_or_lower_version")
          .build();
    }

    log.info("context stored merchantId={} version={}", merchantId, incomingVersion);

    return ContextUpsertResponse.builder()
        .merchantId(merchantId)
        .version(incomingVersion)
        .stored(true)
        .reason("stored")
        .build();
  }

  public ChallengeContextResponse upsertChallenge(ChallengeContextRequest request) {
    String scope = normalizeScope(request.getScope());
    String key = scope + ":" + request.getContextId();
    long incomingVersion = request.getVersion();
    Instant storedAt = Instant.now();
    AtomicBoolean accepted = new AtomicBoolean(false);
    AtomicLong currentVersion = new AtomicLong(incomingVersion);

    challengeContexts.compute(
        key,
        (ignored, existing) -> {
          if (existing != null && existing.getVersion() >= incomingVersion) {
            currentVersion.set(existing.getVersion());
            return existing;
          }

          ChallengeContextEntity next = new ChallengeContextEntity();
          next.setScope(scope);
          next.setContextId(request.getContextId());
          next.setVersion(incomingVersion);
          next.setPayload(request.getPayload());
          next.setDeliveredAt(request.getDeliveredAt());
          next.setStoredAt(storedAt);
          accepted.set(true);
          return next;
        });

    if (!accepted.get()) {
      return ChallengeContextResponse.builder()
          .accepted(false)
          .reason("stale_version")
          .currentVersion(currentVersion.get())
          .build();
    }

    if ("merchant".equals(scope)) {
      indexMerchantPayload(request);
    }

    return ChallengeContextResponse.builder()
        .accepted(true)
        .ackId("ack_" + UUID.randomUUID().toString().replace("-", ""))
        .storedAt(storedAt)
        .build();
  }

  public MerchantContextEntity getRequired(String merchantId) {
    MerchantContextEntity entity = contexts.get(merchantId);
    if (entity == null) {
      throw ApiException.notFound(
          "CONTEXT_NOT_FOUND",
          "No context for merchantId");
    }
    return entity;
  }

  public boolean hasMerchantContext(String merchantId) {
    return contexts.containsKey(merchantId);
  }

  public List<String> merchantIds() {
    return List.copyOf(contexts.keySet());
  }

  public Map<String, Long> contextCountsByScope() {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("category", 0L);
    counts.put("merchant", 0L);
    counts.put("customer", 0L);
    counts.put("trigger", 0L);

    for (ChallengeContextEntity entity : challengeContexts.values()) {
      counts.computeIfPresent(entity.getScope(), (ignored, count) -> count + 1);
    }

    return counts;
  }

  private String toPayload(ContextUpsertRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to serialize merchant context payload",
          e);
    }
  }

  private void indexMerchantPayload(ChallengeContextRequest request) {
    ContextUpsertRequest ctx = toInternalContext(request);
    contexts.compute(
        ctx.getMerchant().getMerchantId(),
        (ignored, existing) -> {
          if (existing != null && existing.getVersion() >= ctx.getVersion()) {
            return existing;
          }

          MerchantContextEntity next = new MerchantContextEntity();
          next.setMerchantId(ctx.getMerchant().getMerchantId());
          next.setVersion(ctx.getVersion());
          next.setPayload(toPayload(ctx));
          next.setUpdatedAt(Instant.now());
          return next;
        });
  }

  private ContextUpsertRequest toInternalContext(ChallengeContextRequest request) {
    JsonNode payload = request.getPayload();
    JsonNode merchantNode = child(payload, "merchant");
    JsonNode performanceNode = child(payload, "performance");
    JsonNode triggerNode = firstChild(payload, "triggerContext", "trigger_context");

    ContextUpsertRequest ctx = new ContextUpsertRequest();
    ctx.setVersion(request.getVersion());
    ctx.setCategory(parseCategory(firstText(payload, "category", "merchant_category")));

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

  private OfferDto toOffer(JsonNode node, int index) {
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

  private static String normalizeScope(String scope) {
    String normalized = scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
    if (List.of("category", "merchant", "customer", "trigger").contains(normalized)) {
      return normalized;
    }
    throw ApiException.badRequest("INVALID_SCOPE", "scope must be category, merchant, customer, or trigger");
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
