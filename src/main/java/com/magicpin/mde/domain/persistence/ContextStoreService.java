package com.magicpin.mde.domain.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.ContextUpsertResponse;
import com.magicpin.mde.api.exception.ApiException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  public ChallengeContextUpsertResult upsertChallengeContext(
      String rawScope,
      String contextId,
      long incomingVersion,
      JsonNode payload,
      Instant deliveredAt) {
    String scope = normalizeScope(rawScope);
    String key = scope + ":" + contextId;
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
          next.setContextId(contextId);
          next.setVersion(incomingVersion);
          next.setPayload(payload);
          next.setDeliveredAt(deliveredAt);
          next.setStoredAt(storedAt);
          accepted.set(true);
          return next;
        });

    return ChallengeContextUpsertResult.builder()
        .accepted(accepted.get())
        .currentVersion(currentVersion.get())
        .storedAt(accepted.get() ? storedAt : null)
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

  private static String normalizeScope(String scope) {
    String normalized = scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
    if (List.of("category", "merchant", "customer", "trigger").contains(normalized)) {
      return normalized;
    }
    throw ApiException.badRequest("INVALID_SCOPE", "scope must be category, merchant, customer, or trigger");
  }
}
