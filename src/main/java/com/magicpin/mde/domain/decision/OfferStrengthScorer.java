package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.OfferDto;
import com.magicpin.mde.api.dto.OfferDto.OfferType;
import com.magicpin.mde.domain.model.Category;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OfferStrengthScorer {
  public OfferPick pickBest(ContextUpsertRequest ctx) {
    if (ctx.getOffers() == null || ctx.getOffers().isEmpty()) {
      return OfferPick.builder()
          .offerId(null)
          .offerSelected("No active offer found")
          .score(15)
          .evidence(Map.of("offer_present", false))
          .build();
    }

    Category cat = ctx.getCategory();
    return ctx.getOffers().stream()
        .map(o -> scoreOffer(o, cat))
        .max(Comparator.comparingInt(OfferPick::getScore))
        .orElseGet(
            () ->
                OfferPick.builder()
                    .offerId(null)
                    .offerSelected("No qualifying offer")
                    .score(20)
                    .evidence(Map.of("offer_present", false))
                    .build());
  }

  private OfferPick scoreOffer(OfferDto o, Category cat) {
    int score = 0;

    if (o.getValidUntil() != null) {
      long days = LocalDate.now().until(o.getValidUntil()).getDays();
      if (days <= 1) score += 18;
      else if (days <= 3) score += 10;
    }

    OfferType t = o.getType();
    if (t == OfferType.FIRST_VISIT) score += 32;
    else if (t == OfferType.FREE_CONSULT) score += 28;
    else if (t == OfferType.TRIAL) score += 24;
    else if (t == OfferType.BOGO) score += 22;
    else score += 14;

    Integer discount = o.getDiscountPercent();
    if (discount != null) {
      if (discount >= 50) score += 26;
      else if (discount >= 35) score += 20;
      else if (discount >= 25) score += 14;
      else if (discount >= 15) score += 8;
    }

    Integer price = o.getPriceInr();
    if (price != null) {
      score += priceAttractiveness(cat, t, price);
    }

    score = Math.max(0, Math.min(100, score));

    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("offer_id", o.getOfferId());
    ev.put("offer_title", o.getTitle());
    ev.put("offer_type", o.getType().name());
    ev.put("offer_price_inr", o.getPriceInr());
    ev.put("offer_discount_percent", o.getDiscountPercent());
    ev.put("offer_valid_until", o.getValidUntil());

    return OfferPick.builder()
        .offerId(o.getOfferId())
        .offerSelected(compactOffer(o))
        .score(score)
        .evidence(ev)
        .build();
  }

  private static int priceAttractiveness(Category cat, OfferType type, int priceInr) {
    return switch (cat) {
      case DENTIST -> {
        if (type == OfferType.FIRST_VISIT && priceInr <= 399) yield 26;
        if (priceInr <= 599) yield 18;
        if (priceInr <= 899) yield 10;
        yield 4;
      }
      case SALON -> {
        if (type == OfferType.TRIAL && priceInr <= 299) yield 24;
        if (priceInr <= 499) yield 16;
        if (priceInr <= 799) yield 10;
        yield 4;
      }
      case RESTAURANT -> {
        if (type == OfferType.BOGO) yield 18;
        if (priceInr <= 199) yield 20;
        if (priceInr <= 299) yield 14;
        yield 6;
      }
      case GYM -> {
        if (type == OfferType.TRIAL && priceInr <= 199) yield 22;
        if (priceInr <= 499) yield 14;
        yield 8;
      }
      case PHARMACY -> {
        if (priceInr <= 149) yield 18;
        if (priceInr <= 299) yield 12;
        yield 6;
      }
      case OTHER -> {
        if (priceInr <= 299) yield 14;
        if (priceInr <= 599) yield 10;
        yield 6;
      }
    };
  }

  private static String compactOffer(OfferDto o) {
    String price = o.getPriceInr() == null ? "" : "₹" + o.getPriceInr();
    String discount =
        o.getDiscountPercent() == null ? "" : (o.getDiscountPercent() + "% off");

    if (!price.isBlank() && !discount.isBlank()) return o.getTitle() + " (" + price + ", " + discount + ")";
    if (!price.isBlank()) return o.getTitle() + " (" + price + ")";
    if (!discount.isBlank()) return o.getTitle() + " (" + discount + ")";
    return o.getTitle();
  }
}

