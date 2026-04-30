package com.magicpin.mde.api.controller;

import com.magicpin.mde.api.dto.ChallengeContextRequest;
import com.magicpin.mde.api.dto.ChallengeContextResponse;
import com.magicpin.mde.api.dto.ChallengeReplyRequest;
import com.magicpin.mde.api.dto.ChallengeReplyResponse;
import com.magicpin.mde.api.dto.ChallengeTickRequest;
import com.magicpin.mde.api.dto.ChallengeTickResponse;
import com.magicpin.mde.domain.decision.ChallengeContractService;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class DecisionController {
  private final ContextStoreService contextStoreService;
  private final ChallengeContractService challengeContractService;

  @PostMapping("/context")
  public ChallengeContextResponse upsertContext(@Valid @RequestBody ChallengeContextRequest req) {
    return contextStoreService.upsertChallenge(req);
  }

  @PostMapping("/tick")
  public ChallengeTickResponse tick(@Valid @RequestBody ChallengeTickRequest req) {
    return challengeContractService.tick(req);
  }

  @PostMapping("/reply")
  public ChallengeReplyResponse reply(@Valid @RequestBody ChallengeReplyRequest req) {
    return challengeContractService.reply(req);
  }
}
