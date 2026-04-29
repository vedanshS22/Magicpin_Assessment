package com.magicpin.mde.api.controller;

import com.magicpin.mde.api.dto.ApiResponse;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.ContextUpsertResponse;
import com.magicpin.mde.api.dto.ReplyRequest;
import com.magicpin.mde.api.dto.ReplyResponse;
import com.magicpin.mde.api.dto.TickRequest;
import com.magicpin.mde.api.dto.TickResponse;
import com.magicpin.mde.domain.decision.DecisionOrchestratorService;
import com.magicpin.mde.domain.persistence.ContextStoreService;
import com.magicpin.mde.util.RequestIdFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class DecisionController {
  private final ContextStoreService contextStoreService;
  private final DecisionOrchestratorService orchestratorService;

  @PostMapping("/context")
  public ApiResponse<ContextUpsertResponse> upsertContext(@Valid @RequestBody ContextUpsertRequest req) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    return ApiResponse.ok(requestId, contextStoreService.upsert(req));
  }

  @PostMapping("/tick")
  public ApiResponse<TickResponse> tick(@Valid @RequestBody TickRequest req) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    return ApiResponse.ok(requestId, orchestratorService.tick(req.getMerchantId()));
  }

  @PostMapping("/reply")
  public ApiResponse<ReplyResponse> reply(@Valid @RequestBody ReplyRequest req) {
    String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    return ApiResponse.ok(requestId, orchestratorService.reply(req.getMerchantId(), req.getText()));
  }
}

