package com.magicpin.mde.api.controller;

import com.magicpin.mde.config.AppProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SystemController {
  private final AppProperties appProperties;

  @GetMapping("/healthz")
  public Map<String, Object> healthz() {
    return Map.of("status", "ok");
  }

  @GetMapping("/metadata")
  public Map<String, Object> metadata() {
    var m = appProperties.getMetadata();
    return Map.of(
        "bot_name", m.getBotName(),
        "version", m.getVersion(),
        "strategy", m.getStrategy(),
        "deterministic", m.isDeterministic());
  }
}