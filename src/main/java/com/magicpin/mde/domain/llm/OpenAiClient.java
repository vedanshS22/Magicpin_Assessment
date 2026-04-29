package com.magicpin.mde.domain.llm;

public interface OpenAiClient {
  String generate(String prompt);
}

