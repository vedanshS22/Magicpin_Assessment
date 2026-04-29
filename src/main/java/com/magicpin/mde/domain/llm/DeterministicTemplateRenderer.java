package com.magicpin.mde.domain.llm;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.decision.MessageStrategy;
import java.util.Objects;

public class DeterministicTemplateRenderer {
  public String render(MessageStrategy s, ContextUpsertRequest ctx) {
    String merchantName = ctx.getMerchant().getMerchantName();
    String keyword = Objects.toString(s.getEvidence().get("top_search_keyword"), "nearby users");
    Integer searches = asInt(s.getEvidence().get("nearby_searches_count_today"));
    Integer convDelta = asInt(s.getEvidence().get("conversion_change_pct_7d"));
    Integer offerPrice = asInt(s.getEvidence().get("offer_price_inr"));
    Integer discount = asInt(s.getEvidence().get("offer_discount_percent"));

    String line1 =
        searches != null && searches > 0
            ? searches + " people nearby searched for " + keyword + " today."
            : "We’re seeing active local intent for " + keyword + " right now.";

    String line2 =
        convDelta != null && convDelta < 0
            ? "Your conversion is down " + (-convDelta) + "% this week — this is the moment to push a sharper hook."
            : s.getMerchantProblem() + ".";

    String offerLine;
    if (offerPrice != null) offerLine = "Lead with " + s.getOfferSelected() + " — make it a clear first-step.";
    else if (discount != null) offerLine = "Lead with " + discount + "% off — keep it simple and first-visit friendly.";
    else offerLine = "Lead with your strongest offer — keep it specific and first-visit friendly.";

    String line3 =
        (merchantName == null || merchantName.isBlank() ? "" : merchantName + ": ") + offerLine;

    return line1 + "\n\n" + line2 + "\n\n" + line3 + "\n\n" + s.getCtaText() + ".";
  }

  private static Integer asInt(Object o) {
    if (o == null) return null;
    if (o instanceof Integer i) return i;
    if (o instanceof Long l) return l.intValue();
    if (o instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(o.toString());
    } catch (Exception ignored) {
      return null;
    }
  }
}

