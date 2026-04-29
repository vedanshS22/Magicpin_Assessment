package com.magicpin.mde.domain.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.ContextUpsertResponse;
import com.magicpin.mde.api.exception.ApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextStoreService {

  private final MerchantContextRepository repository;
  private final ObjectMapper objectMapper;

  @Transactional
  public ContextUpsertResponse upsert(ContextUpsertRequest request) {

    String merchantId = request.getMerchant().getMerchantId();
    long incomingVersion = request.getVersion();

    if (merchantId == null || merchantId.isBlank()) {
      throw ApiException.badRequest(
              "INVALID_MERCHANT_ID",
              "merchant.merchantId is required"
      );
    }

    var existingOpt = repository.findById(merchantId);

    if (existingOpt.isPresent()) {
      var existing = existingOpt.get();

      if (existing.getVersion() >= incomingVersion) {
        return ContextUpsertResponse.builder()
                .merchantId(merchantId)
                .version(existing.getVersion())
                .stored(false)
                .reason("ignored_same_or_lower_version")
                .build();
      }
    }

    String payload;

    try {
      payload = objectMapper.writeValueAsString(request);
    } catch (Exception e) {
      throw new RuntimeException(
              "Failed to serialize merchant context payload",
              e
      );
    }

    MerchantContextEntity entity =
            existingOpt.orElseGet(MerchantContextEntity::new);

    entity.setMerchantId(merchantId);
    entity.setVersion(incomingVersion);
    entity.setPayload(payload);
    entity.setUpdatedAt(Instant.now());

    repository.save(entity);

    log.info(
            "context stored merchantId={} version={}",
            merchantId,
            incomingVersion
    );

    return ContextUpsertResponse.builder()
            .merchantId(merchantId)
            .version(incomingVersion)
            .stored(true)
            .reason("stored")
            .build();
  }

  @Transactional(readOnly = true)
  public MerchantContextEntity getRequired(String merchantId) {
    return repository.findById(merchantId)
            .orElseThrow(() ->
                    ApiException.notFound(
                            "CONTEXT_NOT_FOUND",
                            "No context for merchantId"
                    )
            );
  }
}