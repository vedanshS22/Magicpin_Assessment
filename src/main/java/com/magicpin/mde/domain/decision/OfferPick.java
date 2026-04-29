package com.magicpin.mde.domain.decision;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OfferPick {
  String offerId;
  String offerSelected;
  int score; // 0-100
  Map<String, Object> evidence;
}

