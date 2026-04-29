package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.MerchantPerformanceDto;
import org.springframework.stereotype.Component;

@Component
public class MerchantHealthScorer {
  public int score(ContextUpsertRequest ctx) {
    MerchantPerformanceDto p = ctx.getPerformance();
    int conversion = safe(p.getConversionRatePct(), 0);
    int repeat = safe(p.getRepeatRatePct(), 0);
    int campaign = safe(p.getCampaignResponseRatePct(), 0);
    int bookings = safe(p.getBookingsLast7d(), 0);

    int pain = 0;
    pain += bandLowIsBad(conversion, 10, 18, 25, 32) * 18; // up to 72
    pain += bandLowIsBad(repeat, 12, 20, 28, 35) * 8; // up to 32
    pain += bandLowIsBad(campaign, 5, 10, 16, 22) * 6; // up to 24
    pain += bandLowIsBad(bookings, 2, 5, 10, 18) * 6; // up to 24

    return Math.max(0, Math.min(100, pain));
  }

  public String problemStatement(ContextUpsertRequest ctx) {
    MerchantPerformanceDto p = ctx.getPerformance();
    int conversion = safe(p.getConversionRatePct(), 0);
    Integer delta = p.getConversionChangePct7d();
    int bookings = safe(p.getBookingsLast7d(), 0);

    if (delta != null && delta <= -15) {
      return "Your conversion has dropped sharply recently (down " + (-delta) + "% week-on-week)";
    }
    if (conversion <= 18) {
      return "Your conversion rate is currently low (" + conversion + "%), so demand isn’t turning into bookings";
    }
    if (bookings <= 5) {
      return "Bookings are light this week (" + bookings + " in 7 days), so we should capture demand faster";
    }
    return "There is an opportunity to increase conversions with a tighter offer and timing";
  }

  private static int safe(Integer v, int def) {
    return v == null ? def : v;
  }

  private static int bandLowIsBad(int v, int a, int b, int c, int d) {
    if (v <= a) return 4;
    if (v <= b) return 3;
    if (v <= c) return 2;
    if (v <= d) return 1;
    return 0;
  }
}

