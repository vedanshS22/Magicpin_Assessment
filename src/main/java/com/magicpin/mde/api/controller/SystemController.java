package com.magicpin.mde.api.controller;

import com.magicpin.mde.api.dto.ChallengeHealthResponse;
import com.magicpin.mde.api.dto.ChallengeMetadataResponse;
import com.magicpin.mde.config.AppProperties;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SystemController {
  private final Instant startedAt = Instant.now();
  private final AppProperties appProperties;
  private final ContextStoreService contextStoreService;

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
    var m = appProperties.getMetadata();
    return ChallengeMetadataResponse.builder()
        .teamName(m.getTeamName())
        .teamMembers(m.getTeamMembers())
        .model(m.getModel())
        .approach(m.getApproach())
        .contactEmail(m.getContactEmail())
        .version(m.getVersion())
        .submittedAt(m.getSubmittedAt())
        .build();
  }
}
