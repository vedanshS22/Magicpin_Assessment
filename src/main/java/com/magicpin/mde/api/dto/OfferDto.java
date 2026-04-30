package com.magicpin.mde.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.Data;

@Data
public class OfferDto {
  @NotBlank private String offerId;
  @NotBlank private String title;
  @NotNull private OfferType type;

  @PositiveOrZero private Integer priceInr;
  @PositiveOrZero private Integer originalPriceInr;
  @Min(0) @Max(100) private Integer discountPercent;

  private LocalDate validUntil;
  private String constraints;

  public enum OfferType {
    FIRST_VISIT,
    FREE_CONSULT,
    BOGO,
    FLAT_DISCOUNT,
    PERCENT_DISCOUNT,
    TRIAL
  }
}

