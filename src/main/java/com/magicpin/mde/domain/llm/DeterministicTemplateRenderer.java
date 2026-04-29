package com.magicpin.mde.domain.llm;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.domain.decision.MessageStrategy;
import java.util.Objects;

public class DeterministicTemplateRenderer {

  public String render(MessageStrategy s, ContextUpsertRequest ctx) {

    String merchantName =
            ctx.getMerchant() != null
                    ? ctx.getMerchant().getMerchantName()
                    : null;

    String keyword =
            Objects.toString(
                    s.getEvidence().get("top_search_keyword"),
                    "your service"
            );

    Integer searches =
            asInt(s.getEvidence().get("nearby_searches_count_today"));

    Integer conversionRate =
            asInt(s.getEvidence().get("merchant_conversion_rate_pct"));

    Integer convDelta =
            asInt(s.getEvidence().get("conversion_change_pct_7d"));

    Integer offerPrice =
            asInt(s.getEvidence().get("offer_price_inr"));

    Integer discount =
            asInt(s.getEvidence().get("offer_discount_percent"));

    String line1;

    if (searches != null && searches > 0) {
      line1 =
              searches
                      + " people nearby searched for "
                      + keyword
                      + " today.";
    } else {
      line1 =
              "People nearby are actively searching for "
                      + keyword
                      + " right now.";
    }

    String line2;

    if (conversionRate != null) {
      line2 =
              "Your current conversion rate is only "
                      + conversionRate
                      + "%, so demand is not turning into bookings.";
    } else if (convDelta != null && convDelta < 0) {
      line2 =
              "Your conversion is down "
                      + (-convDelta)
                      + "% this week, which means high-intent customers are slipping away.";
    } else {
      line2 =
              "This is the right moment to convert nearby demand into real bookings.";
    }

    String line3;

    if (offerPrice != null) {
      line3 =
              "Should we launch your ₹"
                      + offerPrice
                      + " first-visit offer tonight?";
    } else if (discount != null) {
      line3 =
              "Should we launch your "
                      + discount
                      + "% off first-visit offer tonight?";
    } else {
      line3 =
              "Should we launch your strongest first-visit offer tonight?";
    }

    String line4 =
            (merchantName == null || merchantName.isBlank())
                    ? s.getCtaText() + " to start."
                    : merchantName + " — " + s.getCtaText() + " to start.";

    return
            line1
                    + "\n\n"
                    + line2
                    + "\n\n"
                    + line3
                    + "\n\n"
                    + line4;
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