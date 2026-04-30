package com.magicpin.mde.api.controller;

import com.magicpin.mde.api.dto.ChallengeContextRequest;
import com.magicpin.mde.api.dto.ChallengeContextResponse;
import com.magicpin.mde.api.dto.ChallengeHealthResponse;
import com.magicpin.mde.api.dto.ChallengeMetadataResponse;
import com.magicpin.mde.api.dto.ChallengeReplyRequest;
import com.magicpin.mde.api.dto.ChallengeReplyResponse;
import com.magicpin.mde.api.dto.ChallengeTickRequest;
import com.magicpin.mde.api.dto.ChallengeTickResponse;
import com.magicpin.mde.api.mapper.MagicpinJudgeMapper;
import com.magicpin.mde.config.AppProperties;
import com.magicpin.mde.domain.decision.ChallengeContractService;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MagicpinJudgeController {
  private final Instant startedAt = Instant.now();

  private final ContextStoreService contextStoreService;
  private final ChallengeContractService challengeContractService;
  private final MagicpinJudgeMapper mapper;
  private final AppProperties appProperties;

  @PostMapping("/context")
  public ChallengeContextResponse context(@Valid @RequestBody ChallengeContextRequest request) {
    var result =
        contextStoreService.upsertChallengeContext(
            request.getScope(),
            request.getContextId(),
            request.getVersion(),
            request.getPayload(),
            request.getDeliveredAt());

    if (!result.isAccepted()) {
      return mapper.staleContext(result.getCurrentVersion());
    }

    if (mapper.mapsToMerchantContext(request)) {
      contextStoreService.upsert(mapper.toContextUpsertRequest(request));
    }

    return mapper.acceptedContext(result.getStoredAt());
  }

  @PostMapping("/tick")
  public ChallengeTickResponse tick(@Valid @RequestBody ChallengeTickRequest request) {
    return challengeContractService.tick(request);
  }

  @PostMapping("/reply")
  public ChallengeReplyResponse reply(@Valid @RequestBody ChallengeReplyRequest request) {
    return challengeContractService.reply(request);
  }

  @GetMapping("/healthz")
  public ChallengeHealthResponse healthz() {
    return ChallengeHealthResponse.builder()
        .status("ok")
        .uptimeSeconds(Duration.between(startedAt, Instant.now()).toSeconds())
        .contextsLoaded(contextStoreService.contextCountsByScope())
        .build();
  }

  @GetMapping("/metadata")
  public ChallengeMetadataResponse metadata() {
    var metadata = appProperties.getMetadata();
    return ChallengeMetadataResponse.builder()
        .teamName(metadata.getTeamName())
        .teamMembers(metadata.getTeamMembers())
        .model(metadata.getModel())
        .approach(metadata.getApproach())
        .contactEmail(metadata.getContactEmail())
        .version(metadata.getVersion())
        .submittedAt(metadata.getSubmittedAt())
        .build();
  }
}
